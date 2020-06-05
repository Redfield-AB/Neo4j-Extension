/**
 *
 */
package se.redfield.knime.neo4j.writer;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.ConnectorSpec;
import se.redfield.knime.neo4j.db.AsyncRunnerLauncher;
import se.redfield.knime.neo4j.db.Neo4jDataConverter;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.db.ScriptResult;
import se.redfield.knime.neo4j.json.JsonBuilder;
import se.redfield.knime.neo4j.utils.FlowVariablesProvider;
import se.redfield.knime.neo4j.utils.ModelUtils;

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
        new WriterConfigSerializer().write(config, settings);
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
            table = executeFromTableSource(exec, config.getInputColumn(), tableInput, neo4j);
        } else {
            table = executeFromScriptSource(exec, neo4j);
        }

        final BufferedDataTable executionResultPort = exec.createBufferedDataTable(table,
                exec.createSubExecutionContext(0.0));
        return new PortObject[] {executionResultPort, connectorPort};
    }

    private DataTable executeFromTableSource(
            final ExecutionContext exec, final String inputColumn,
            final BufferedDataTable inputTable, final Neo4jSupport neo4j) throws Exception {
        final List<String> scripts = ModelUtils.getStringsFromTextColumn(inputTable, inputColumn, this);
        final Driver driver = neo4j.createDriver();

        try {
            final Map<Long, String> results = config.isUseAsync() ?
                    executeAsync(neo4j, scripts, driver)
                    : executeSync(neo4j, scripts, driver);

            //build result
            final List<DataRow> rows = new LinkedList<DataRow>();
            long rowNum = 0;
            for (final DataRow origin : inputTable) {
                rows.add(createRow(origin, results.get(rowNum)));
                rowNum++;
            }

            return createTable(exec, ModelUtils.createSpecWithAddedJsonColumn(
                    inputTable.getSpec(), config.getInputColumn()), rows);
        } finally {
            driver.closeAsync();
        }
    }

    private Map<Long, String> executeSync(final Neo4jSupport neo4j,
            final List<String> scripts, final Driver driver) throws Exception {
        final Map<Long, String> results = new HashMap<>();
        long row = 0;
        for (final String script : scripts) {
            try {
                results.put(row, runSingleScript(driver, script));
            } catch (final Exception e) {
                results.put(row, createErrorJson(e.getMessage()));
                if (config.isStopOnQueryFailure()) {
                    getLogger().error(SOME_QUERIES_ERROR);
                    throw new Exception(SOME_QUERIES_ERROR);
                } else {
                    setWarningMessage(SOME_QUERIES_ERROR);
                }
            }
            row++;
        }
        return results;
    }
    private Map<Long, String> executeAsync(final Neo4jSupport neo4j,
            final List<String> scripts, final Driver driver)
            throws Exception {
        //create thread ID to transaction map.
        final Map<Long, ThransactionWithSession> transactions = new HashMap<>();

        final AsyncRunnerLauncher<String, String> runner = new AsyncRunnerLauncher<>(
                s -> runScriptInAsyncContext(driver, s, transactions));
        runner.setStopOnQueryFailure(config.isStopOnQueryFailure());

        final Map<Long, String> results = runner.run(scripts,
                neo4j.getConfig().getAdvancedSettings().getMaxConnectionPoolSize());
        if (runner.hasErrors()) {
            if (config.isStopOnQueryFailure()) {
                //rollback all transactions
                for (final ThransactionWithSession tr : transactions.values()) {
                    tr.rollbackAndClose();
                }

                //log error
                getLogger().error(SOME_QUERIES_ERROR);

                throw new Exception(SOME_QUERIES_ERROR);
            } else {
                setWarningMessage(SOME_QUERIES_ERROR);
            }
        }

        //commit all transactions
        for (final ThransactionWithSession tr : transactions.values()) {
            tr.commitAndClose();
        }

        return results;
    }

    /**
     * @param driver Neo4j driver.
     * @param script script to execute.
     * @param trs map of transactions.
     * @return
     */
    private ScriptResult<String> runScriptInAsyncContext(final Driver driver, final String script,
            final Map<Long, ThransactionWithSession> trs) {
        //get ID of current worker thread
        final long threadId = Thread.currentThread().getId();

        ThransactionWithSession tr = trs.get(threadId);
        if (tr == null) {
            //create transaction for given worker thread
            final Session s = driver.session();
            final Transaction t = s.beginTransaction();
            tr = new ThransactionWithSession(s, t);

            //save transaction only for stop on failure mode, because
            //in case of error all actions should be rolled back at the end
            //of asynchronous execution of all scripts.
            if (config.isStopOnQueryFailure()) {
                trs.put(threadId, tr);
            }
        }

        List<Record> records;
        try {
            //run script in given transaction context
            final Result run = tr.getTransaction().run(script);
            records = run.list();

            //if not in stop on query failure mode, then should commit result
            //immediately
            if (!config.isStopOnQueryFailure()) {
                tr.commitAndClose();
            }
        } catch (final RuntimeException e) {
            //if not in stop on query failure mode, then should rollback result
            //immediately because transaction is not supplied
            if (!config.isStopOnQueryFailure()) {
                tr.rollbackAndClose();
                return new ScriptResult<String>(createErrorJson(e.getMessage()), e);
            } else {
                throw e;
            }
        }

        //build JSON result from records.
        final String result = createSuccessJson(records, new Neo4jDataConverter(driver.defaultTypeSystem()));
        return new ScriptResult<String>(result);
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
    private DataTable createTable(final ExecutionContext exec, final DataTableSpec tableSpec,
            final List<DataRow> rows) {
        final BufferedDataContainer table = exec.createDataContainer(tableSpec);
        try {
            for (final DataRow row : rows) {
                table.addRowToTable(row);
            }
        } finally {
            table.close();
        }
        return table.getTable();
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
