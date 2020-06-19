/**
 *
 */
package se.redfield.knime.neo4j.writer;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import se.redfield.knime.neo4j.async.AsyncRunner;
import se.redfield.knime.neo4j.async.AsyncRunnerLauncher;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.ConnectorSpec;
import se.redfield.knime.neo4j.db.ContextListeningDriver;
import se.redfield.knime.neo4j.db.Neo4jDataConverter;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.db.ScriptExecutionResult;
import se.redfield.knime.neo4j.db.WithSessionRunner;
import se.redfield.knime.neo4j.json.JsonBuilder;
import se.redfield.knime.neo4j.model.FlowVariablesProvider;
import se.redfield.knime.neo4j.model.ModelUtils;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class WriterModel extends NodeModel implements FlowVariablesProvider {
    public static final String SOME_QUERIES_ERROR = "Some queries were not successfully executed.";
    private WriterConfig config;

    public WriterModel() {
        super(new PortType[] {BufferedDataTable.TYPE_OPTIONAL, ConnectorPortObject.TYPE},
                new PortType[] {BufferedDataTable.TYPE, ConnectorPortObject.TYPE});
        this.config = new WriterConfig();
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {}

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {}

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        new WriterConfigSerializer().save(config, settings);
    }
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //prevalidation is not required
    }
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        config = new WriterConfigSerializer().read(settings);
        //add metadata
    }
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs.length < 2 || !(inSpecs[1] instanceof ConnectorSpec)) {
            throw new InvalidSettingsException("Not Neo4j input found");
        }

        final DataTableSpec inputTableSpec = (DataTableSpec) inSpecs[0];

        final DataTableSpec spec;
        if (inputTableSpec != null) {
            spec = ModelUtils.createSpecWithAddedJsonColumn(
                    inputTableSpec, config.getInputColumn());
        } else {
            spec = createOneColumnSpec();
        }

        return new PortObjectSpec[] {spec, inSpecs[1]};
    }
    @Override
    protected PortObject[] execute(final PortObject[] input, final ExecutionContext exec) throws Exception {
        final BufferedDataTable tableInput = (BufferedDataTable) input[0];
        final ConnectorPortObject connectorPort = (ConnectorPortObject) input[1];

        final Neo4jSupport neo4j = new Neo4jSupport(connectorPort.getPortData().createResolvedConfig(
                getCredentialsProvider()));

        DataTable table;
        if (tableInput != null) {
            table = executeFromTableSource(exec, tableInput, neo4j);
        } else {
            table = executeFromScriptSource(exec, neo4j);
        }

        final BufferedDataTable executionResultPort = exec.createBufferedDataTable(table,
                exec.createSubExecutionContext(0.0));
        return new PortObject[] {executionResultPort, connectorPort};
    }

    private DataTable executeFromTableSource(
            final ExecutionContext exec, final BufferedDataTable inputTable,
            final Neo4jSupport neo4j) throws Exception {
        final DataTableSpec tableSpec = ModelUtils.createSpecWithAddedJsonColumn(
                inputTable.getSpec(), config.getInputColumn());
        final BufferedDataContainer table = exec.createDataContainer(tableSpec);
        try {
            final ContextListeningDriver driver = neo4j.createDriver(exec);

            try {
                if (config.isUseAsync()) {
                    executeFromTableSourceAsync(inputTable, driver, neo4j, table);
                } else {
                    executeFromTableSourceSync(inputTable, driver, table);
                }
            } finally {
                driver.close();
            }
        } finally {
            table.close();
        }
        return table.getTable();
    }

    private void executeFromTableSourceSync(final BufferedDataTable inputTable,
            final ContextListeningDriver driver, final BufferedDataContainer output)
            throws Exception {

        driver.setProgress(0.);
        final Session session = driver.getDriver().session();
        try {
            final Neo4jDataConverter converter = new Neo4jDataConverter(
                    driver.getDriver().defaultTypeSystem());

            long pos = 0;
            final long size = inputTable.size();
            for (final DataRow origin : inputTable) {
                ScriptExecutionResult res;

                final String script = getScriptFromInputColumn(inputTable, origin);

                final Transaction tx = session.beginTransaction();
                try {
                    final Result run = tx.run(script);
                    res = new ScriptExecutionResult(origin, run.list(), null);

                    commitAndClose(tx);
                } catch (final Exception e) {
                    res = new ScriptExecutionResult(origin, null, e);
                    rollbackAndClose(tx);

                    if (config.isStopOnQueryFailure()) {
                        throw new Exception(SOME_QUERIES_ERROR);
                    } else {
                        setWarningMessage(SOME_QUERIES_ERROR);
                    }
                }

                driver.setProgress((double) pos / size);
                pos++;
                //build result outside of Neo4j transaction.
                addResultToOutput(res, output, converter);
            }
        } finally {
            session.close();
        }
    }
    private void addResultToOutput(final ScriptExecutionResult res, final BufferedDataContainer output,
            final Neo4jDataConverter converter) {
        final List<Record> result = res.recors;
        Throwable error = res.error;
        if(error == null) {
            try {
                output.addRowToTable(createRow(res.row, createSuccessJson(result, converter)));
            } catch (final IOException e) {
                error = e;
            }
        }
        if (error != null) {
            try {
                output.addRowToTable(createRow(res.row, createErrorJson(error.getMessage())));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private String getScriptFromInputColumn(final BufferedDataTable inputTable, final DataRow origin) {
        final int colIndex = inputTable.getDataTableSpec().findColumnIndex(config.getInputColumn());
        final StringCell cell = (StringCell) origin.getCell(colIndex);
        return ModelUtils.insertFlowVariables(cell.getStringValue(), this);
    }

    private void executeFromTableSourceAsync(final BufferedDataTable inputTable,
            final ContextListeningDriver driver, final Neo4jSupport neo4j, final BufferedDataContainer output)
            throws Exception, IOException {
        final int maxConnectionPoolSize = neo4j.getConfig().getMaxConnectionPoolSize();
        final long tableSize = inputTable.size();
        final int numThreads = Math.max((int) Math.min(maxConnectionPoolSize, tableSize), 1);
        final boolean stopOnQueryFailure = config.isStopOnQueryFailure();

        //create output synchronizer
        final Neo4jDataConverter converter = new Neo4jDataConverter(driver.getDriver().defaultTypeSystem());

        //create asynchronous scripts runner
        final AsyncRunner<DataRow, ScriptExecutionResult> r = new WithSessionRunner<>(
                (session, query) -> runSingleScriptInAsyncContext(
                        session, inputTable, query),
                driver.getDriver());

        driver.setProgress(0.);
        final AtomicLong counter = new AtomicLong();
        final AsyncRunnerLauncher<DataRow, ScriptExecutionResult> runner
                = AsyncRunnerLauncher.Builder.<DataRow, ScriptExecutionResult>newBuilder()
            .withRunner(r)
            .withSource(inputTable.iterator())
            .withConsumer(res -> {
                final long num = counter.getAndIncrement();
                driver.setProgress((double) num / tableSize);
                addResultToOutput(res, output, converter);
            })
            .withNumThreads(numThreads)
            .withStopOnFailure(stopOnQueryFailure)
            .withKeepSourceOrder(config.isKeepSourceOrder())
            .withMaxBufferSize(maxConnectionPoolSize * 2)
            .build();

        //run scripts in parallel
        runner.run();
        //check errors
        if (runner.hasErrors()) {
            if (stopOnQueryFailure) {
                throw new Exception(SOME_QUERIES_ERROR);
            } else {
                setWarningMessage(SOME_QUERIES_ERROR);
            }
        }
    }

    private ScriptExecutionResult runSingleScriptInAsyncContext(final Session session,
            final BufferedDataTable inputTable, final DataRow row) throws IOException {
        final Transaction tr = session.beginTransaction();
        ScriptExecutionResult res;
        try {
            //run script in given transaction context
            final Result run = tr.run(getScriptFromInputColumn(inputTable, row));
            final List<Record> records = run.list();

            //build JSON result from records.
            res = new ScriptExecutionResult(row, records, null);

            //if not in stop on query failure mode, then should commit result
            //immediately
            commitAndClose(tr);
        } catch (final RuntimeException e) {
            rollbackAndClose(tr);
            if (!config.isStopOnQueryFailure()) {
                res = new ScriptExecutionResult(row, null, e);
            } else {
                throw e;
            }
        }

        return res;
    }
    /**
     * @param tr transaction.
     */
    private void rollbackAndClose(final Transaction tr) {
        try {
            tr.rollback();
        } catch (final Throwable e) {
        }
        try {
            tr.close();
        } catch (final Throwable e) {
        }
    }
    /**
     * @param tr transaction.
     */
    private void commitAndClose(final Transaction tr) {
        try {
            tr.commit();
        } catch (final Throwable e) {
        }
        try {
            tr.close();
        } catch (final Throwable e) {
        }
    }
    private String runSingleScript(final Driver driver, final String script) {
        //run script in context of transaction.
        final List<Record> records = Neo4jSupport.runWithSession(driver, s -> s.writeTransaction(tx -> {
            final Result run = tx.run(script);
            final List<Record> res = run.list();
            tx.commit();
            return res;
        }));
        //build JSON result from records.
        return createSuccessJson(records, new Neo4jDataConverter(driver.defaultTypeSystem()));
    }
    /**
     * @param originRow origin table row.
     * @param json JSON to create additional column.
     * @return compound row with origin row and appended JSON column.
     * @throws IOException
     */
    private DataRow createRow(final DataRow originRow, final String json) throws IOException {
        DataCell cell;
        if (json != null) {
            cell = createJsonCell(json);
        } else {
            cell = createErrorCell("Is not executed since other script failed");
        }
        return new AppendedColumnRow(originRow, cell);
    }
    /**
     * @param error error description.
     * @return JSON cell with error description.
     * @throws IOException
     */
    private DataCell createErrorCell(final String error) throws IOException {
        return createJsonCell(createErrorJson(error));
    }

    private DataTable executeFromScriptSource(final ExecutionContext exec, final Neo4jSupport neo4j)
            throws Exception {
        if (config.getScript() == null) {
            final String error = "Cypher script is not specified but also not input table connected";
            setWarningMessage(error);
            throw new InvalidSettingsException(error);
        }

        final Driver driver = neo4j.createDriver();

        String json;
        try {
            json = runSingleScript(driver, config.getScript());
        } catch (final Exception e) {
            if (config.isStopOnQueryFailure()) {
                throw e;
            } else {
                setWarningMessage(e.getMessage());
                json = createErrorJson(e.getMessage());
            }
        } finally {
            driver.closeAsync();
        }
        //convert output to JSON.
        return createJsonTable(json, exec);
    }

    /**
     * @param error error description.
     * @return JSON with error.
     */
    private String createErrorJson(final String error) {
        final StringWriter wr = new StringWriter();

        final JsonGenerator gen = Json.createGenerator(wr);
        gen.writeStartObject();

        gen.write("status", "error");
        gen.write("description", error);

        gen.writeEnd();
        gen.close();

        return wr.toString();
    }
    public static String createSuccessJson(final List<Record> records, final Neo4jDataConverter adapter) {
        final StringWriter wr = new StringWriter();

        final JsonGenerator gen = Json.createGenerator(wr);
        gen.writeStartObject();

        gen.write("status", "success");
        gen.writeKey("result");
        JsonBuilder.writeJson(records, gen, adapter);

        gen.writeEnd();
        gen.close();

        return wr.toString();
    }
    private DataTable createJsonTable(final String json, final ExecutionContext exec) throws IOException {
        final DataTableSpec tableSpec = createOneColumnSpec();
        final DefaultRow row = new DefaultRow(new RowKey("result"), createJsonCell(json));
        final BufferedDataContainer table = exec.createDataContainer(tableSpec);
        try {
            table.addRowToTable(row);
        } finally {
            table.close();
        }

        return table.getTable();
    }
    private DataCell createJsonCell(final String json) throws IOException {
        return JSONCellFactory.create(json, false);
    }
    private DataTableSpec createOneColumnSpec() {
        //one row, one string column
        final DataColumnSpec stringColumn = new DataColumnSpecCreator("result", JSONCell.TYPE).createSpec();
        final DataTableSpec tableSpec = new DataTableSpec(stringColumn);
        return tableSpec;
    }
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        return new BufferedDataTable[0]; // just disable
    }
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) {
        return new DataTableSpec[0]; //just disable
    }
    @Override
    protected void reset() {
    }
}
