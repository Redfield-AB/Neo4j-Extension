/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.OperationNotSupportedException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultRowIterator;
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
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableFunctionProducer;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.util.Pair;

import se.redfield.knime.neo4j.async.AsyncRunner;
import se.redfield.knime.neo4j.async.AsyncRunnerLauncher;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.db.ContextListeningDriver;
import se.redfield.knime.neo4j.db.Neo4jDataConverter;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.db.WithSessionRunner;
import se.redfield.knime.neo4j.json.JsonBuilder;
import se.redfield.knime.neo4j.model.FlowVariablesProvider;
import se.redfield.knime.neo4j.model.ModelUtils;
import se.redfield.knime.neo4j.table.DataTypeDetection;
import se.redfield.knime.neo4j.table.Neo4jTableOutputSupport;
import se.redfield.knime.neo4j.table.RowInputContainer;
import se.redfield.knime.neo4j.table.RowInputIterator;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderModel extends NodeModel implements FlowVariablesProvider, StreamableFunctionProducer {
    private static final String NOT_READ_ONLY_ERROR = "Query has not only read actions therefore transaction is rolled back";
    public static final String SOME_QUERIES_ERROR = "Some queries were not successfully executed.";
    private ReaderConfig config;

    public ReaderModel(final NodeCreationConfiguration creationConfig) {
        super(creationConfig.getPortConfig().get().getInputPorts(),
                creationConfig.getPortConfig().get().getOutputPorts());
        this.config = new ReaderConfig();
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {}

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {}

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        new ReaderConfigSerializer().save(config, settings);
    }
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //prevalidation is not required
    }
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        config = new ReaderConfigSerializer().read(settings);
    }
    @Override
    public StreamableFunction createStreamableOperator(final PartitionInfo partitionInfo,
            final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableFunction() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs,
                    final ExecutionContext exec) throws Exception {
                RowInput input = null;
                if (inputs.length > 0) {
                    input = (RowInput) inputs[0];
                }

                //check correct input
                if (input == null) {
                    throw new Exception("Streaming only available for table input");
                }

                //create Neo4j support
                final ConnectorPortObject connectorPort
                    = (ConnectorPortObject) ((PortObjectInput) inputs[inputs.length - 1])
                        .getPortObject();
                //forward connection
                ((PortObjectOutput) outputs[outputs.length - 1]).setPortObject(connectorPort);

                final Neo4jSupport neo4j = new Neo4jSupport(connectorPort.getPortData()
                        .createResolvedConfig(getCredentialsProvider()));

                //execute
                executeFromInputTableToJson(exec,
                        new RowInputContainer(input), (RowOutput) outputs[0], neo4j);
            }

            @Override
            public DataRow compute(final DataRow input) throws Exception {
                throw new OperationNotSupportedException();
            }
        };
    }
    @Override
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] roles = super.getInputPortRoles();
        if (getNrInPorts() > 1) {
            roles[0] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        }
        return roles;
    }
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        PortObjectSpec conSpec;
        DataTableSpec tableSpec = null;

        if (inSpecs.length > 1) {
            conSpec = inSpecs[1];
            tableSpec = (DataTableSpec) inSpecs[0];
        } else {
            conSpec = inSpecs[0];
        }

        PortObjectSpec output = null;
        //if JSON output used it is possible to specify output.
        if (tableSpec != null) {
            output = ModelUtils.createSpecWithAddedJsonColumn(tableSpec, config.getInputColumn());
        } else if (config.isUseJson()) {
            output = ModelUtils.createOneColumnJsonTableSpec("json");
        }
        return new PortObjectSpec[] {output, conSpec};
    }
    @Override
    protected PortObject[] execute(final PortObject[] input, final ExecutionContext exec) throws Exception {
        BufferedDataTable tableInput = null;
        final ConnectorPortObject connectorPort;

        if (input.length > 1) {
            tableInput = (BufferedDataTable) input[0];
        }
        connectorPort = (ConnectorPortObject) input[input.length - 1];

        final Neo4jSupport neo4j = new Neo4jSupport(connectorPort.getPortData().createResolvedConfig(
                getCredentialsProvider()));

        final boolean useTableInput = tableInput != null;
        DataTable table;
        if (!useTableInput) {
            table = executeFromScriptSource(exec, neo4j);
        } else {
            table = executeFromTableSource(exec, tableInput, neo4j);
        }

        final BufferedDataTable executionResultPort = exec.createBufferedDataTable(table,
                exec.createSubExecutionContext(0.0));
        return new PortObject[] {
                executionResultPort,
                connectorPort //forward connection
        };
    }

    private DataTable executeFromTableSource(
            final ExecutionContext exec, final BufferedDataTable inputTable,
            final Neo4jSupport neo4j) throws Exception {
        final BufferedDataContainer table = exec.createDataContainer(ModelUtils.createSpecWithAddedJsonColumn(
                inputTable.getSpec(), config.getInputColumn()));
        try {
            if (inputTable.size() > 0) {
                executeFromInputTableToJson(exec,
                        new RowInputContainer(new DataTableRowInput(inputTable)),
                        new BufferedDataTableRowOutput(table),
                        neo4j);
            }
        } finally {
            table.close();
        }
        return table.getTable();
    }

    private void executeFromInputTableToJson(final ExecutionContext exec,
            final RowInputContainer input,
            final RowOutput output, final Neo4jSupport neo4j) throws Exception {
        final int columnIndex = input.getInput().getDataTableSpec().findColumnIndex(
                config.getInputColumn());

        final ContextListeningDriver driver = neo4j.createDriver(exec);
        try {
            final AsyncRunner<DataRow, DataRow> r = new WithSessionRunner<>(
                    (session, row) -> runScriptFromColumn(session, driver.getDriver(), row, columnIndex),
                    driver.getDriver());
            final long tableSize = input.getRowCount();
            final AtomicLong counter = new AtomicLong();
            driver.setProgress(0.);

            //build runner.
            final AsyncRunnerLauncher<DataRow, DataRow> runner = AsyncRunnerLauncher.Builder.newBuilder(r)
                .withConsumer(row -> {
                    try {
                        output.push(row);
                        if (input.hasRowCount()) {
                            final double p = counter.getAndIncrement() / (double) tableSize;
                            driver.setProgress(p);
                        }
                    } catch (final Throwable e) {
                        setWarningMessage(e.getMessage());
                    }
                })
                .withKeepSourceOrder(config.isKeepSourceOrder())
                .withSource(new RowInputIterator(input.getInput()))
                .withNumThreads(Math.max(1, (int) Math.min(
                        neo4j.getConfig().getMaxConnectionPoolSize(), tableSize)))
                .withStopOnFailure(config.isStopOnQueryFailure())
                .build();

            //run asynchronously
            runner.run();
            if (runner.hasErrors()) {
                if (config.isStopOnQueryFailure()) {
                    getLogger().error(SOME_QUERIES_ERROR);
                    throw new Exception(SOME_QUERIES_ERROR);
                } else {
                    setWarningMessage(SOME_QUERIES_ERROR);
                }
            }
        } finally {
            driver.close();
        }
    }
    private DataRow runScriptFromColumn(final Session session, final Driver driver,
            final DataRow row, final int columnIndex) throws Throwable {
        try {
            final StringCell cell = (StringCell) row.getCell(columnIndex);
            final String query  = ModelUtils.insertFlowVariables(cell.getStringValue(),
                    ReaderModel.this);

            final List<Record> records = Neo4jSupport.runInReadOnlyTransaction(
                    session, query, null);
            final String json = buildJson(records, new Neo4jDataConverter(driver.defaultTypeSystem()));
            return ModelUtils.createRowWithAppendedJson(row, json);
        } catch (final Throwable e) {
            if (config.isStopOnQueryFailure()) {
                throw new Exception(SOME_QUERIES_ERROR);
            }
            setWarningMessage(SOME_QUERIES_ERROR);
            return new AppendedColumnRow(row, new MissingCell(e.getMessage()));
        }
    }

    private DataTable executeFromScriptSource(final ExecutionContext exec, final Neo4jSupport neo4j)
            throws Exception {
        DataTable table;
        if (config.getScript() == null) {
            final String error = "Cypher script is not specified but also not input table connected";
            setWarningMessage(error);
            throw new InvalidSettingsException(error);
        }

        final Driver driver = neo4j.createDriver();
        try {
            final String[] warning = {null};
            List<Record> records;
            try {
                records = Neo4jSupport.runRead(driver, ModelUtils.insertFlowVariables(config.getScript(), this),
                        () -> warning[0] = NOT_READ_ONLY_ERROR);
                if (warning[0] != null) {
                    setWarningMessage(warning[0]);
                }
            } catch (final Exception e) {
                if (config.isStopOnQueryFailure()) {
                    throw e;
                } else {
                    setWarningMessage(e.getMessage());
                    records = new LinkedList<>();
                }
            }
            if (config.isUseJson()) {
                //convert output to JSON.
                final String json = buildJson(records, new Neo4jDataConverter(driver.defaultTypeSystem()));
                table = createJsonTable(json, exec);
            } else if (records.isEmpty()) {
                table = createEmptyTable();
            } else {
                table = createDataTable(records, exec, new Neo4jDataConverter(driver.defaultTypeSystem()));
            }
        } finally {
            driver.closeAsync();
        }

        return table;
    }
    private DataTable createEmptyTable() {
        final DataTableSpec spec = new DataTableSpec("Empty Result");
        final DefaultRowIterator rows = new DefaultRowIterator();
        return new DataTable() {

            @Override
            public RowIterator iterator() {
                return rows;
            }

            @Override
            public DataTableSpec getDataTableSpec() {
                return spec;
            }
        };
    }
    private DataTable createDataTable(final List<Record> records,
            final ExecutionContext exec, final Neo4jDataConverter converter) throws Exception {
        final Neo4jTableOutputSupport support = new Neo4jTableOutputSupport(converter);

        final DataTableSpec tableSpec = createTableSpec(support, records);
        final List<DataRow> rows = createDataRows(support, tableSpec, records);
        return createTable(exec, tableSpec, rows);
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
        final DataTableSpec tableSpec = ModelUtils.createOneColumnJsonTableSpec("json");
        final DefaultRow row = new DefaultRow(new RowKey("json"),
                JSONCellFactory.create(json, false));

        final BufferedDataContainer table = exec.createDataContainer(tableSpec);
        try {
            table.addRowToTable(row);
        } finally {
            table.close();
        }

        return table.getTable();
    }
    public String buildJson(final List<Record> records, final Neo4jDataConverter adapter) {
        return new JsonBuilder(adapter).buildJson(records);
    }
    private DataTableSpec createTableSpec(final Neo4jTableOutputSupport support, final List<Record> records) {
        DataTypeDetection[] detections = null;

        //attempt to populate with best types
        for (final Record record : records) {
            final List<Pair<String, Value>> fields = record.fields();

            //create specs array
            if (detections == null) {
                detections = new DataTypeDetection[fields.size()];
            }

            int i = 0;
            for (final Pair<String,Value> pair : fields) {
                final DataTypeDetection dt = support.detectCompatibleCellType(pair.value());
                if (detections[i] == null) {
                    detections[i] = dt;
                } else {
                    detections[i].update(dt);
                }
                i++;
            }
        }

        //create types from detections
        final DataColumnSpec[] columns = new DataColumnSpec[detections.length];
        int i = 0;
        for (final Pair<String,Value> pair : records.get(0).fields()) {
            final DataTypeDetection det = detections[i];
            final String name = pair.key();
            DataType type = null;

            if (det.isDetected()) {
                if (det.isList()) {
                    type = ListCell.getCollectionType(det.calculateType());
                } else {
                    type = det.calculateType();
                }
            }

            if (type == null) {
                if (det.isList()) {
                    type = ListCell.getCollectionType(JSONCell.TYPE);
                } else {
                    type = StringCell.TYPE;
                }
            }

            final DataColumnSpecCreator creator = new DataColumnSpecCreator(name, type);
            columns[i] = creator.createSpec();
            i++;
        }

        return new DataTableSpec(columns);
    }
    /**
     * @param support type system.
     * @param tableSpec table specification.
     * @param records record list.
     * @return list of data rows.
     * @throws Exception
     */
    private List<DataRow> createDataRows(final Neo4jTableOutputSupport support,
            final DataTableSpec tableSpec, final List<Record> records) throws Exception {
        int index = 0;
        final List<DataRow> rows = new LinkedList<DataRow>();
        for (final Record r : records) {
            rows.add(createDataRow(support, tableSpec, index, r));
            index++;
        }
        return rows;
    }
    /**
     * @param adapter data adapter.
     * @param columnSpec column specification.
     * @param index row index.
     * @param r record.
     * @return data row.
     * @throws Exception
     */
    private DataRow createDataRow(final Neo4jTableOutputSupport adapter,
            final DataTableSpec tableSpec, final int index, final Record r) throws Exception {
        final DataCell[] cells = new DataCell[r.size()];
        for (int i = 0; i < cells.length; i++) {
            final DataColumnSpec columnSpec = tableSpec.getColumnSpec(i);
            cells[i] = adapter.createCell(columnSpec.getType(), r.get(i));
        }
        return new DefaultRow(new RowKey("r-" + index), cells);
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
