/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
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
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
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
import org.neo4j.driver.Value;
import org.neo4j.driver.summary.Notification;
import org.neo4j.driver.util.Pair;

import se.redfield.knime.json.JsonBuilder;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.ConnectorSpec;
import se.redfield.knime.neo4j.db.DataAdapter;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.reader.async.AsyncScriptRunner;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfig;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderModel extends NodeModel implements FlowVariablesProvider {
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
        new ReaderConfigSerializer().write(config, settings);
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
        final ConnectorSpec connectorSpec = (ConnectorSpec) inSpecs[1];

        PortObjectSpec output = null;
        //if JSON output used it is possible to specify output.
        final boolean useTableInput = inSpecs[0] instanceof DataTableSpec;
        if (useTableInput) {
            output = createSpecWithAddedJsonColumn((DataTableSpec) inSpecs[0]);
        } else if (config.isUseJson()) {
            output = createOneColumnJsonOutputSpec("json");
        }

        return new PortObjectSpec[] {output, connectorSpec};
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
            final ExecutionContext exec, final ColumnInfo inputColumn,
            final BufferedDataTable inputTable, final Neo4jSupport neo4j) throws Exception {
        final List<String> scripts = getScriptsToLaunch(inputTable, inputColumn);
        final Driver driver = neo4j.createDriver();
        final Map<Long, String> results;

        try {
            final AsyncScriptRunner runner = new AsyncScriptRunner(driver);
            runner.setStopOnQueryFailure(config.isStopOnQueryFailure());
            results = runner.run(scripts,
                    neo4j.getConfig().getAdvancedSettings().getMaxConnectionPoolSize());
            if (runner.hasErrors()) {
                if (config.isStopOnQueryFailure()) {
                    getLogger().error(SOME_QUERIES_ERROR);
                    throw new Exception(SOME_QUERIES_ERROR);
                } else {
                    setWarningMessage(SOME_QUERIES_ERROR);
                }
            }
        } finally {
            driver.closeAsync();
        }

        //build result
        final List<DataRow> rows = new LinkedList<DataRow>();
        long rowNum = 0;
        for (final DataRow origin : inputTable) {
            rows.add(createExpandedRow(origin, results.get(rowNum)));
            rowNum++;
        }

        return createTable(exec, createSpecWithAddedJsonColumn(inputTable.getSpec()), rows);
    }

    private List<String> getScriptsToLaunch(final BufferedDataTable inputTable, final ColumnInfo inputColumn) {
        final List<String> scripts = new LinkedList<>();
        for (final DataRow row : inputTable) {
            String script = null;
            if (inputColumn != null) {
                final StringCell cell = (StringCell) row.getCell(inputColumn.getOffset());
                script = UiUtils.insertFlowVariables(cell.getStringValue(), this);
            }
            scripts.add(script);
        }
        return scripts;
    }

    private DataRow createExpandedRow(final DataRow originRow, final String json) {
        if (json != null) {
            try {
                return new AppendedColumnRow(originRow, JSONCellFactory.create(json, false));
            } catch (final IOException e) {
            }
        }
        return new AppendedColumnRow(originRow, new MissingCell("Not a value"));
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

        final String[] warning = {null};
        final List<Record> records = Neo4jSupport.runRead(driver,
                UiUtils.insertFlowVariables(config.getScript(), this),
                n -> warning[0] = buildWarning(n));
        if (warning[0] != null) {
            setWarningMessage(warning[0]);
        }

        if (config.isUseJson()) {
            //convert output to JSON.
            final String json = buildJson(records, new DataAdapter(driver.defaultTypeSystem()));
            table = createJsonTable(json, exec);
        } else if (records.isEmpty()) {
            table = createEmptyTable();
        } else {
            table = createDataTable(records, exec, new DataAdapter(driver.defaultTypeSystem()));
        }
        return table;
    }

    public static String buildWarning(final List<Notification> notifs) {
        final StringBuilder sb = new StringBuilder();
        if (notifs != null && !notifs.isEmpty()) {
            for (final Notification n : notifs) {
                final String desc = n.description();
                if (desc != null) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(desc);
                }
            }
        }

        if (sb.length() == 0){
            sb.append("Query has not only read actions therefore transaction is rolled back");
        }
        return sb.toString();
    }

    private DataTable createEmptyTable() {
        return new DataTableImpl(new DataTableSpec("Empty Result"), new DefaultRowIterator());
    }
    private DataTable createDataTable(final List<Record> records,
            final ExecutionContext exec, final DataAdapter adapter) throws Exception {
        final DataTableSpec tableSpec = createTableSpec(adapter,  records);
        final List<DataRow> rows = createDateRows(records, adapter);
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
        final DataTableSpec tableSpec = createOneColumnJsonOutputSpec("json");
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
    private DataTableSpec createOneColumnJsonOutputSpec(final String columnName) {
        //one row, one string column
        final DataColumnSpec stringColumn = new DataColumnSpecCreator(columnName, JSONCell.TYPE).createSpec();
        final DataTableSpec tableSpec = new DataTableSpec(stringColumn);
        return tableSpec;
    }
    public static String buildJson(final List<Record> records, final DataAdapter adapter) {
        final StringWriter wr = new StringWriter();

        final JsonGenerator gen = Json.createGenerator(wr);
        new JsonBuilder(adapter).writeJson(records, gen);
        gen.flush();

        return wr.toString();
    }
    private DataTableSpec createTableSpec(final DataAdapter adapter, final List<Record> records) {
        DataColumnSpec[] columns = null;
        boolean hasNull = false;

        //attempt to populate with best types
        for (final Record record : records) {
            final List<Pair<String, Value>> fields = record.fields();

            if (columns == null) {
                columns = new DataColumnSpec[fields.size()];
            }

            int i = 0;
            for (final Pair<String,Value> pair : fields) {
                hasNull = false;
                if (columns[i] == null) {
                    final String name = pair.key();
                    final Value v = pair.value();
                    if (v.isNull()) {
                        hasNull = true;
                    } else {
                        final DataColumnSpecCreator creator = new DataColumnSpecCreator(name,
                                adapter.getCompatibleType(v));
                        columns[i] = creator.createSpec();
                    }
                }

                i++;
            }

            if (!hasNull) {
                break;
            }
        }

        if (hasNull) {
            //set values for nulls
            int i = 0;
            for (final Pair<String,Value> pair : records.get(0).fields()) {
                if (columns[i] == null) {
                    final String name = pair.key();
                    final Value v = pair.value();
                    final DataColumnSpecCreator creator = new DataColumnSpecCreator(name,
                            adapter.getCompatibleType(v));
                    columns[i] = creator.createSpec();
                }
                i++;
            }
        }

        return new DataTableSpec(columns);
    }
    /**
     * @param records record list.
     * @param adapter type system.
     * @return list of data rows.
     * @throws Exception
     */
    private List<DataRow> createDateRows(final List<Record> records,
            final DataAdapter adapter) throws Exception {
        int index = 0;
        final List<DataRow> rows = new LinkedList<DataRow>();
        for (final Record r : records) {
            rows.add(createDataRow(r, adapter, index));
            index++;
        }
        return rows;
    }
    /**
     * @param r record.
     * @param adapter data adapter.
     * @param index row index.
     * @return data row.
     * @throws Exception
     */
    private DataRow createDataRow(final Record r,
            final DataAdapter adapter, final int index) throws Exception {
        final DataCell[] cells = new DataCell[r.size()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = adapter.createCell(r.get(i));
        }
        return new DefaultRow(new RowKey("r-" + index), cells);
    }

    public DataTableSpec createSpecWithAddedJsonColumn(final DataTableSpec origin) {
        String resultColumn = "results";
        if (config.getInputColumn() != null) {
            resultColumn = config.getInputColumn().getName() + ":result";
        }
        return new DataTableSpec("result", origin, createOneColumnJsonOutputSpec(resultColumn));
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
