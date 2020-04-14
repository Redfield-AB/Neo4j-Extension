/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultRowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
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
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.driver.util.Pair;

import se.redfield.knime.neo4jextension.cfg.ReaderConfig;
import se.redfield.knime.neo4jextension.cfg.ReaderConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JReaderModel extends NodeModel {
    private ReaderConfig config;

    public Neo4JReaderModel() {
        super(new PortType[] {ConnectorPortObject.TYPE},
                new PortType[] {BufferedDataTable.TYPE, ConnectorPortObject.TYPE_OPTIONAL});
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
    protected PortObject[] execute(final PortObject[] input, final ExecutionContext exec) throws Exception {
        final ConnectorPortObject neo4j = (ConnectorPortObject) input[0];

        final List<Record> records = neo4j.run(config.getScript());

        DataTable table;
        if (records.isEmpty()) {
            table = createEmptyTable();
        } else if (config.isUseJson()) {
            table = createJsonTable(exec, neo4j.getTypeSystem(), records);
        } else {
            table = createDataTable(exec, neo4j.getTypeSystem(), records);
        }

        return new PortObject[] {
                exec.createBufferedDataTable(table,
                        exec.createSubExecutionContext(0.0)),
                neo4j //forward connection
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
            final TypeSystem typeSystem, final List<Record> records) throws Exception {
        final DataTableSpec tableSpec = createTableSpec(typeSystem,  records.get(0));
        final List<DataRow> rows = createDateRows(typeSystem, records);

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
    private DataTable createJsonTable(final ExecutionContext exec, final TypeSystem typeSystem,
            final List<Record> records) throws IOException {
        //one row, one string column
        final DataColumnSpec stringColumn = new DataColumnSpecCreator("json", StringCell.TYPE).createSpec();
        final DataTableSpec tableSpec = new DataTableSpec(stringColumn);

        //convert output to JSON.
        final String json = buildJson(records, typeSystem);
        final DefaultRow row = new DefaultRow(new RowKey("json"),
                new StringCell(json));

        final BufferedDataContainer table = exec.createDataContainer(tableSpec);
        try {
            table.addRowToTable(row);
        } finally {
            table.close();
        }

        return table.getTable();
    }
    /**
     * @param records
     * @param typeSystem
     * @return
     */
    private String buildJson(final List<Record> records, final TypeSystem typeSystem) {
        final StringWriter wr = new StringWriter();

        final JsonGenerator gen = Json.createGenerator(wr);
        new JsonBuilder(typeSystem).writeJson(records, gen);
        gen.flush();

        return wr.toString();
    }

    /**
     * @param record
     * @return
     */
    private DataTableSpec createTableSpec(final TypeSystem typeSystem, final Record record) {
        final List<Pair<String, Value>> fields = record.fields();
        final DataColumnSpec[] columns = new DataColumnSpec[fields.size()];

        int i = 0;
        for (final Pair<String,Value> pair : fields) {
            final String name = pair.key();
            final Value v = pair.value();
            final DataColumnSpecCreator creator = new DataColumnSpecCreator(name,
                    getType(typeSystem, v));
            columns[i] = creator.createSpec();
            i++;
        }

        return new DataTableSpec(columns);
    }
    /**
     * @param typeSystem type system.
     * @param records record list.
     * @return list of data rows.
     * @throws Exception
     */
    private List<DataRow> createDateRows(final TypeSystem typeSystem,
            final List<Record> records) throws Exception {
        int index = 0;
        final List<DataRow> rows = new LinkedList<DataRow>();
        for (final Record r : records) {
            rows.add(createDataRow(typeSystem, index, r));
            index++;
        }
        return rows;
    }
    /**
     * @param typeSystem type system.
     * @param index row index.
     * @param r record.
     * @return data row.
     * @throws Exception
     */
    private DataRow createDataRow(final TypeSystem typeSystem,
            final int index, final Record r) throws Exception {
        final DataCell[] cells = new DataCell[r.size()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = createCell(typeSystem, r.get(i));
        }
        return new DefaultRow(new RowKey("r-" + index), cells);
    }
    /**
     * @param typeSystem type system.
     * @param value value
     * @return data cell.
     */
    private DataCell createCell(final TypeSystem typeSystem, final Value value)
            throws Exception {
        if (value == null) {
            return null;
        }

        if (typeSystem.ANY().isTypeOf(value)) {
            return new StringCell(value.toString());
        } else if (typeSystem.BOOLEAN().isTypeOf(value)) {
            return value.asBoolean() ? BooleanCell.TRUE : BooleanCell.FALSE;
        } else if (typeSystem.BYTES().isTypeOf(value)) {
            return new BinaryObjectCellFactory().create(value.asByteArray());
        } else if (typeSystem.STRING().isTypeOf(value)) {
            return new StringCell(value.asString());
        } else if (typeSystem.NUMBER().isTypeOf(value)) {
            return new DoubleCell(value.asDouble());
        } else if (typeSystem.INTEGER().isTypeOf(value)) {
            return new IntCell(value.asInt());
        } else if (typeSystem.FLOAT().isTypeOf(value)) {
            return new DoubleCell(value.asDouble());
        } else if (typeSystem.LIST().isTypeOf(value)) {
            return new StringCell(value.toString());
        } else if (typeSystem.MAP().isTypeOf(value)) {
            return new StringCell(value.toString());
        } else if (typeSystem.NODE().isTypeOf(value)) {
            return new StringCell(value.toString());
        } else if (typeSystem.RELATIONSHIP().isTypeOf(value)) {
            return new StringCell(value.toString());
        } else if (typeSystem.PATH().isTypeOf(value)) {
            return new StringCell(value.toString());
        } else if (typeSystem.POINT().isTypeOf(value)) {
            return new StringCell(value.toString());
        } else if (typeSystem.DATE().isTypeOf(value)) {
            return new LocalDateCellFactory().createCell(formatDate(value.asZonedDateTime()));
        } else if (typeSystem.TIME().isTypeOf(value)) {
            return new LocalTimeCellFactory().createCell(formatTime(value.asZonedDateTime()));
        } else if (typeSystem.LOCAL_TIME().isTypeOf(value)) {
            return new LocalTimeCellFactory().createCell(formatTime(value.asLocalTime()));
        } else if (typeSystem.LOCAL_DATE_TIME().isTypeOf(value)) {
            return new LocalDateTimeCellFactory().createCell(formatDate(value.asLocalDateTime()));
        } else if (typeSystem.DATE_TIME().isTypeOf(value)) {
            return new LocalDateTimeCellFactory().createCell(formatDate(value.asLocalDateTime()));
        } else if (typeSystem.DURATION().isTypeOf(value)) {
            return DurationCellFactory.create(
                    Duration.ofNanos(value.asIsoDuration().nanoseconds()));
        } else if (typeSystem.NULL().isTypeOf(value)) {
            return null;
        }

        return new StringCell(value.toString());
    }
    private DataType getType(final TypeSystem typeSystem, final Value value) {
        if (typeSystem.ANY().isTypeOf(value)) {
            return StringCell.TYPE;
        } else if (typeSystem.BOOLEAN().isTypeOf(value)) {
            return BooleanCell.TYPE;
        } else if (typeSystem.BYTES().isTypeOf(value)) {
            return BinaryObjectDataCell.TYPE;
        } else if (typeSystem.STRING().isTypeOf(value)) {
            return StringCell.TYPE;
        } else if (typeSystem.NUMBER().isTypeOf(value)) {
            return DoubleCell.TYPE;
        } else if (typeSystem.INTEGER().isTypeOf(value)) {
            return IntCell.TYPE;
        } else if (typeSystem.FLOAT().isTypeOf(value)) {
            return DoubleCell.TYPE;
        } else if (typeSystem.LIST().isTypeOf(value)) {
            return StringCell.TYPE;
        } else if (typeSystem.MAP().isTypeOf(value)) {
            return StringCell.TYPE;
        } else if (typeSystem.NODE().isTypeOf(value)) {
            return StringCell.TYPE;
        } else if (typeSystem.RELATIONSHIP().isTypeOf(value)) {
            return StringCell.TYPE;
        } else if (typeSystem.PATH().isTypeOf(value)) {
            return StringCell.TYPE;
        } else if (typeSystem.POINT().isTypeOf(value)) {
            return StringCell.TYPE;
        } else if (typeSystem.DATE().isTypeOf(value)) {
            return LocalDateCellFactory.TYPE;
        } else if (typeSystem.TIME().isTypeOf(value)) {
            return LocalTimeCellFactory.TYPE;
        } else if (typeSystem.LOCAL_TIME().isTypeOf(value)) {
            return LocalTimeCellFactory.TYPE;
        } else if (typeSystem.LOCAL_DATE_TIME().isTypeOf(value)) {
            return LocalDateTimeCellFactory.TYPE;
        } else if (typeSystem.DATE_TIME().isTypeOf(value)) {
            return LocalDateTimeCellFactory.TYPE;
        } else if (typeSystem.DURATION().isTypeOf(value)) {
            return DurationCellFactory.TYPE;
        } else if (typeSystem.NULL().isTypeOf(value)) {
            return StringCell.TYPE;
        }

        return StringCell.TYPE;
    }

    private String formatDate(final LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }
    private String formatTime(final LocalTime localTime) {
        return localTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    private String formatTime(final ChronoZonedDateTime<?> dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    private String formatDate(final ChronoZonedDateTime<?> dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        return new BufferedDataTable[0]; // just disable
    }
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs.length < 1) {
            throw new InvalidSettingsException("Not input found");
        }
        return new PortObjectSpec[] {
                null,
                inSpecs[0] //forward connection
        };
    }
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) {
        return new DataTableSpec[0]; //just disable
    }
    @Override
    protected void reset() {
    }
}
