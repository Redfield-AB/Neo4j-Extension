/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.util.Pair;

import se.redfield.knime.neo4j.async.AsyncRunner;
import se.redfield.knime.neo4j.async.AsyncRunnerLauncher;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.ConnectorSpec;
import se.redfield.knime.neo4j.db.ContextListeningDriver;
import se.redfield.knime.neo4j.db.Neo4jDataConverter;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.db.WithSessionRunner;
import se.redfield.knime.neo4j.json.JsonBuilder;
import se.redfield.knime.neo4j.model.FlowVariablesProvider;
import se.redfield.knime.neo4j.model.ModelUtils;
import se.redfield.knime.neo4j.table.DataTypeDetection;
import se.redfield.knime.neo4j.table.Neo4jTableOutputSupport;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderModel extends NodeModel implements FlowVariablesProvider {
    private static final String NOT_READ_ONLY_ERROR = "Query has not only read actions therefore transaction is rolled back";
    public static final String SOME_QUERIES_ERROR = "Some queries were not successfully executed.";
    private ReaderConfig config;

    public ReaderModel() {
        super(new PortType[] {BufferedDataTable.TYPE_OPTIONAL, ConnectorPortObject.TYPE},
                new PortType[] {BufferedDataTable.TYPE, ConnectorPortObject.TYPE});
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
        //add metadata
    }
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs.length < 2 || !(inSpecs[1] instanceof ConnectorSpec)) {
            throw new InvalidSettingsException("Not Neo4j input found");
        }

        PortObjectSpec output = null;
        //if JSON output used it is possible to specify output.
        final boolean useTableInput = inSpecs[0] instanceof DataTableSpec;
        if (useTableInput) {
            output = ModelUtils.createSpecWithAddedJsonColumn(
                    (DataTableSpec) inSpecs[0], config.getInputColumn());
        } else if (config.isUseJson()) {
            output = ModelUtils.createOneColumnJsonTableSpec("json");
        }

        return new PortObjectSpec[] {output, inSpecs[1]};
    }
    @Override
    protected PortObject[] execute(final PortObject[] input, final ExecutionContext exec) throws Exception {
        final BufferedDataTable tableInput = (BufferedDataTable) input[0];
        final ConnectorPortObject connectorPort = (ConnectorPortObject) input[1];

        final Neo4jSupport neo4j = new Neo4jSupport(connectorPort.getPortData().createResolvedConfig(
                getCredentialsProvider()));

        final boolean useTableInput = tableInput != null;
        DataTable table;
        if (!useTableInput) {
            table = executeFromScriptSource(exec, neo4j);
        } else {
            table = executeFromTableSource(exec, config.getInputColumn(), tableInput, neo4j);
        }

        final BufferedDataTable executionResultPort = exec.createBufferedDataTable(table,
                exec.createSubExecutionContext(0.0));
        return new PortObject[] {
                executionResultPort,
                connectorPort //forward connection
        };
    }

    private DataTable executeFromTableSource(
            final ExecutionContext exec, final String inputColumn,
            final BufferedDataTable inputTable, final Neo4jSupport neo4j) throws Exception {
        final ContextListeningDriver driver = neo4j.createDriver(exec);
        final int columnIndex = inputTable.getDataTableSpec().findColumnIndex(inputColumn);

        final BufferedDataContainer table = exec.createDataContainer(ModelUtils.createSpecWithAddedJsonColumn(
                inputTable.getSpec(), config.getInputColumn()));
        try {
            try {
                final AsyncRunner<DataRow, DataRow> r = new WithSessionRunner<>(
                        (session, row) -> runScriptFromColumn(session, driver.getDriver(), row, columnIndex),
                        driver.getDriver());
                final long tableSize = inputTable.size();
                final AtomicLong counter = new AtomicLong();
                driver.setProgress(0.);

                final AsyncRunnerLauncher<DataRow, DataRow> runner = AsyncRunnerLauncher.Builder.newBuilder(r)
                    .withConsumer(row -> {
                        table.addRowToTable(row);
                        final double p = counter.getAndIncrement() / (double) tableSize;
                        driver.setProgress(p);
                    })
                    .withKeepSourceOrder(config.isKeepSourceOrder())
                    .withSource(inputTable.iterator())
                    .withNumThreads((int) Math.min(
                            neo4j.getConfig().getMaxConnectionPoolSize(), tableSize))
                    .withStopOnFailure(config.isStopOnQueryFailure())
                    .build();

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
        } finally {
            table.close();
        }
        return table.getTable();
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
