/**
 *
 */
package se.redfield.knime.neo4j.table;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.knime.core.data.DataCell;
import org.knime.core.data.MissingCell;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;

import se.redfield.knime.neo4j.db.ConvertedValueConsumer;
import se.redfield.knime.neo4j.db.Neo4jDataConverter;
import se.redfield.knime.neo4j.json.Neo4jValueWriter;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public abstract class Neo4jCellFactory implements ConvertedValueConsumer {
    private List<DataCell> currentListTypes;
    private final boolean useJson;

    public Neo4jCellFactory(final boolean useJson) {
        super();
        this.useJson = useJson;
    }

    @Override
    public void acceptBoolean(final boolean b) {
        acceptCellInternal(b ? BooleanCell.TRUE : BooleanCell.FALSE);
    }
    @Override
    public void acceptBytes(final byte[] bytes) {
        DataCell cell;
        try {
            cell = new BinaryObjectCellFactory().create(bytes);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        acceptCellInternal(cell);
    }
    @Override
    public void acceptString(final String str) {
        acceptCellInternal(new StringCell(str));
    }
    @Override
    public void acceptNumber(final Number num) {
        acceptCellInternal(new DoubleCell(num.doubleValue()));
    }
    @Override
    public void acceptInteger(final long value) {
        acceptCellInternal(new LongCell(value));
    }
    @Override
    public void acceptFloat(final double value) {
        acceptCellInternal(new DoubleCell(value));
    }
    @Override
    public void acceptList(final List<Object> list) {
        if (isInList()) {
            acceptCell(createJson(list));
            return;
        }
        currentListTypes = new LinkedList<>();
        if (!list.isEmpty()) {
            for (final Object obj : list) {
                if (useJson) {
                    acceptCellInternal(createJson(obj));
                } else {
                    acceptObject(obj);
                }
            }
        }

        acceptCell(CollectionCellFactory.createListCell(currentListTypes));
        currentListTypes = null;
    }
    @Override
    public void acceptMap(final Map<String, Object> map) {
        acceptCellInternal(createJson(map));
    }
    @Override
    public void acceptNode(final Node node) {
        acceptMap(node.asMap());
    }
    @Override
    public void acceptRelationship(final Relationship rel) {
        acceptMap(rel.asMap());
    }
    @Override
    public void acceptPath(final Path path) {
        acceptString(String.valueOf(path));
    }
    @Override
    public void acceptPoint(final Point p) {
        final Map<String, Object> map = new HashMap<>();
        map.put("x", p.x());
        map.put("y", p.y());
        map.put("z", p.z());
        map.put("srid", p.srid());
        acceptMap(map);
    }
    @Override
    public void acceptDate(final LocalDate d) {
        acceptCellInternal(LocalDateCellFactory.create(d));
    }
    @Override
    public void acceptLocalTime(final LocalTime time) {
        acceptCellInternal(LocalTimeCellFactory.create(time));
    }
    @Override
    public void acceptLocalDateTime(final LocalDateTime time) {
        acceptCellInternal(LocalDateTimeCellFactory.create(time));
    }
    @Override
    public void acceptDurationMilliseconds(final long duration) {
        acceptCellInternal(DurationCellFactory.create(Duration.ofMillis(duration)));
    }
    @Override
    public void acceptNull() {
        acceptCellInternal(new MissingCell("null"));
    }
    private void acceptCellInternal(final DataCell cell) {
        if (isInList()) {
            currentListTypes.add(cell);
        } else {
            acceptCell(cell);
        }
    }
    private boolean isInList() {
        return currentListTypes != null;
    }
    protected abstract void acceptCell(DataCell cell);
    @Override
    public void acceptUndefined(final Object value) {
        acceptString(String.valueOf(value));
    }
    protected DataCell createJson(final Object value) {
        return createJson(getConverter(), value);
    }
    /**
     * @param converter converter.
     * @param value value to create data cell.
     * @return data cell.
     */
    public static DataCell createJson(final Neo4jDataConverter converter, final Object value) {
        if (value == null || (value instanceof Value
                && ((Value) value).isNull())) {
            return new MissingCell("null JSON");
        }
        final StringWriter sw = new StringWriter();
        final JsonGenerator gen = Json.createGenerator(sw);

        final Neo4jValueWriter wr = new Neo4jValueWriter(gen, converter);
        wr.acceptObject(value);
        gen.flush();

        final String json = sw.toString();
        try {
            return JSONCellFactory.create(json, false);
        } catch (final IOException e) {
            return new MissingCell(e.getMessage());
        }
    }
}
