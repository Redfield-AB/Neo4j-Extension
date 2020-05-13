/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
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
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.summary.Notification;
import org.neo4j.driver.util.Pair;

import se.redfield.knime.json.JsonBuilder;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.ConnectorSpec;
import se.redfield.knime.neo4j.db.DataAdapter;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfig;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderModel extends NodeModel {
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
        final boolean useTableInput = inSpecs[0] != null;
        if (useTableInput || config.isUseJson()) {
            output = createJsonOutputSpec();
        }

        return new PortObjectSpec[] {output, connectorSpec};
    }
    @Override
    protected PortObject[] execute(final PortObject[] input, final ExecutionContext exec) throws Exception {
        final BufferedDataTable tableInput = (BufferedDataTable) input[0];
        final ConnectorPortObject portObject = (ConnectorPortObject) input[1];

        final Neo4jSupport neo4j = new Neo4jSupport(portObject.getPortData().createResolvedConfig(
                getCredentialsProvider()));

        final boolean useTableInput = tableInput != null;
        DataTable table;
        if (!useTableInput) {
            final String[] warning = {null};
            final List<Record> records = neo4j.runRead(insertFlowVariables(config.getScript()),
                    n -> warning[0] = buildWarning(n));
            if (warning[0] != null) {
                setWarningMessage(warning[0]);
            }

            if (config.isUseJson()) {
                table = createJsonTable(exec, neo4j.createDataAdapter(), records);
            } else if (records.isEmpty()) {
                table = createEmptyTable();
            } else {
                table = createDataTable(exec, neo4j.createDataAdapter(), records);
            }
        } else {
            final ColumnInfo inputColumn = config.getInputColumn();
            if (inputColumn == null) {
                table = createJsonTable(exec, neo4j.createDataAdapter(), new LinkedList<Record>());
            } else {
                final List<String> rowKeys = new LinkedList<>();
                final Map<String, List<Record>> result = new HashMap<>();

                boolean error = false;
                for (final DataRow dataRow : tableInput) {
                    final StringCell cell = (StringCell) dataRow.getCell(inputColumn.getOffset());
                    final String script = cell.getStringValue();

                    final String rowKey = dataRow.getKey().getString();
                    rowKeys.add(rowKey);
                    result.put(rowKey, null);

                    if (script == null) {
                        error = true;
                    } else {
                        final boolean[] hasError = {false};
                        final List<Record> records = neo4j.runRead(insertFlowVariables(config.getScript()),
                                n -> hasError[0] = true);

                        if (!hasError[0]) {
                            result.put(rowKey, records);
                        }

                        error = error || hasError[0];
                    }
                }

                table = createDataTable(exec, neo4j.createDataAdapter(), rowKeys, result);
                if (error) {
                    setWarningMessage("Some queries were not successfully executed.");
                }
            }
        }

        return new PortObject[] {
                exec.createBufferedDataTable(table,
                        exec.createSubExecutionContext(0.0)),
                portObject //forward connection
        };
    }

    private String buildWarning(final List<Notification> notifs) {
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

    /**
     * @param script
     * @return
     */
    private String insertFlowVariables(final String script) {
        final Map<String, FlowVariable> vars = getAvailableFlowVariables(getFlowVariableTypes());

        final int[] indexes = getSortedVarOccurences(script, vars);

        final StringBuilder sb = new StringBuilder(script);
        for (int i = indexes.length - 1; i >= 0; i--) {
            final int offset = indexes[i];
            final int end = sb.indexOf("}}", offset + 2) + 2;

            final String var = sb.substring(offset + 3, end - 2);
            sb.replace(offset, end, vars.get(var).getValueAsString());
        }

        return sb.toString();
    }

    /**
     * @param script
     * @param vars
     * @return
     */
    private int[] getSortedVarOccurences(final String script, final Map<String, FlowVariable> vars) {
        int pos = 0;
        final List<Integer> offsets = new LinkedList<Integer>();
        while (true) {
            final int offset = script.indexOf("${{", pos);
            if (offset < 0) {
                break;
            }

            final int end = script.indexOf("}}", offset);
            if (end < 0) {
                break;
            }

            offsets.add(offset);
            pos = end;
        }

        //convert to int array
        final int[] result = new int[offsets.size()];
        int i = 0;
        for (final Integer o : offsets) {
            result[i] = o.intValue();
            i++;
        }
        return result;
    }

    /**
     * @return
     */
    private DataTable createEmptyTable() {
        return new DataTable() {
            /** {@inheritDoc} */
            @Override
            public DataTableSpec getDataTableSpec() {
                return new DataTableSpec("Empty Result");
            }

            /** {@inheritDoc} */
            @Override
            public RowIterator iterator() {
                return new DefaultRowIterator();
            }
        };
    }

    private DataTable createDataTable(final ExecutionContext exec,
            final DataAdapter adapter, final List<Record> records) throws Exception {
        final DataTableSpec tableSpec = createTableSpec(adapter,  records);
        final List<DataRow> rows = createDateRows(adapter, records);

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
    private DataTable createJsonTable(final ExecutionContext exec, final DataAdapter adapter,
            final List<Record> records) throws IOException {
        //convert output to JSON.
        final String json = buildJson(records, adapter);
        return createJsonTable(json, exec);
    }
    private DataTable createDataTable(final ExecutionContext exec, final DataAdapter adapter,
            final List<String> rowKeys, final Map<String, List<Record>> records) throws IOException {
        //convert output to JSON.
        final String json = buildJson(rowKeys, records, adapter);
        return createJsonTable(json, exec);
    }
    private DataTable createJsonTable(final String json, final ExecutionContext exec) throws IOException {
        final DataTableSpec tableSpec = createJsonOutputSpec();
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
    private DataTableSpec createJsonOutputSpec() {
        //one row, one string column
        final DataColumnSpec stringColumn = new DataColumnSpecCreator("json", JSONCell.TYPE).createSpec();
        final DataTableSpec tableSpec = new DataTableSpec(stringColumn);
        return tableSpec;
    }
    private String buildJson(final List<Record> records, final DataAdapter adapter) {
        final StringWriter wr = new StringWriter();

        final JsonGenerator gen = Json.createGenerator(wr);
        new JsonBuilder(adapter).writeJson(records, gen);
        gen.flush();

        return wr.toString();
    }
    private String buildJson(final List<String> rowKeys, final Map<String, List<Record>> records,
            final DataAdapter adapter) {
        final StringWriter wr = new StringWriter();

        final JsonGenerator gen = Json.createGenerator(wr);
        new JsonBuilder(adapter).writeJson(rowKeys, records, gen);
        gen.flush();

        return wr.toString();
    }

    /**
     * @param record
     * @return
     */
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
     * @param adapter type system.
     * @param records record list.
     * @return list of data rows.
     * @throws Exception
     */
    private List<DataRow> createDateRows(final DataAdapter adapter,
            final List<Record> records) throws Exception {
        int index = 0;
        final List<DataRow> rows = new LinkedList<DataRow>();
        for (final Record r : records) {
            rows.add(createDataRow(adapter, index, r));
            index++;
        }
        return rows;
    }
    /**
     * @param adapter data adapter.
     * @param index row index.
     * @param r record.
     * @return data row.
     * @throws Exception
     */
    private DataRow createDataRow(final DataAdapter adapter,
            final int index, final Record r) throws Exception {
        final DataCell[] cells = new DataCell[r.size()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = adapter.createCell(r.get(i));
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
    /**
     * @return all flow variable types.
     */
    @SuppressWarnings("rawtypes")
    public static VariableType[] getFlowVariableTypes() {
        final Set<VariableType<?>> types = new HashSet<>();
        types.add(VariableType.BooleanArrayType.INSTANCE);
        types.add(VariableType.BooleanType.INSTANCE);
        types.add(VariableType.CredentialsType.INSTANCE);
        types.add(VariableType.DoubleArrayType.INSTANCE);
        types.add(VariableType.BooleanArrayType.INSTANCE);
        types.add(VariableType.DoubleArrayType.INSTANCE);
        types.add(VariableType.DoubleType.INSTANCE);
        types.add(VariableType.IntArrayType.INSTANCE);
        types.add(VariableType.IntType.INSTANCE);
        types.add(VariableType.LongArrayType.INSTANCE);
        types.add(VariableType.LongType.INSTANCE);
        types.add(VariableType.StringArrayType.INSTANCE);
        types.add(VariableType.StringType.INSTANCE);

        return types.toArray(new VariableType[types.size()]);
    }
}
