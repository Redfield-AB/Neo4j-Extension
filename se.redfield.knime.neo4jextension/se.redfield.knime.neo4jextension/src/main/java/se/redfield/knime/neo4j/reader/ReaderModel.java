/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

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
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
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
        new ReaderConfigSerializer().read(settings);
    }
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        config = new ReaderConfigSerializer().read(settings);
    }
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs.length < 2 || !(inSpecs[1] instanceof ConnectorSpec)) {
            throw new InvalidSettingsException("Not Neo4j input found");
        }

        return new PortObjectSpec[] {
                null,
                inSpecs[1] //forward connection
        };
    }
    @Override
    protected PortObject[] execute(final PortObject[] input, final ExecutionContext exec) throws Exception {
        final ConnectorPortObject portObject = (ConnectorPortObject) input[1];
        final Neo4jSupport neo4j = new Neo4jSupport(portObject.getPortData().getConnectorConfig());

        final List<Record> records = neo4j.runRead(config.getScript());

        DataTable table;
        if (records.isEmpty()) {
            table = createEmptyTable();
        } else if (config.isUseJson()) {
            table = createJsonTable(exec, neo4j.createDataAdapter(), records);
        } else {
            table = createDataTable(exec, neo4j.createDataAdapter(), records);
        }

        return new PortObject[] {
                exec.createBufferedDataTable(table,
                        exec.createSubExecutionContext(0.0)),
                portObject //forward connection
        };
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
        final DataTableSpec tableSpec = createTableSpec(adapter,  records.get(0));
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
        //one row, one string column
        final DataColumnSpec stringColumn = new DataColumnSpecCreator("json", JSONCell.TYPE).createSpec();
        final DataTableSpec tableSpec = new DataTableSpec(stringColumn);

        //convert output to JSON.
        final String json = buildJson(records, adapter);
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
    private String buildJson(final List<Record> records, final DataAdapter adapter) {
        final StringWriter wr = new StringWriter();

        final JsonGenerator gen = Json.createGenerator(wr);
        new JsonBuilder(adapter).writeJson(records, gen);
        gen.flush();

        return wr.toString();
    }

    /**
     * @param record
     * @return
     */
    private DataTableSpec createTableSpec(final DataAdapter adapter, final Record record) {
        final List<Pair<String, Value>> fields = record.fields();
        final DataColumnSpec[] columns = new DataColumnSpec[fields.size()];

        int i = 0;
        for (final Pair<String,Value> pair : fields) {
            final String name = pair.key();
            final Value v = pair.value();
            final DataColumnSpecCreator creator = new DataColumnSpecCreator(name,
                    adapter.getCompatibleType(v));
            columns[i] = creator.createSpec();
            i++;
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
}
