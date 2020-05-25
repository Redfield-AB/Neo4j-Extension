/**
 *
 */
package se.redfield.knime.table;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.knime.core.data.DataCell;
import org.knime.core.data.MissingCell;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;

import se.redfield.knime.json.Neo4jValueWriter;
import se.redfield.knime.neo4j.db.ConvertedValueConsumer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public interface Neo4jCellFactory extends ConvertedValueConsumer {
    @Override
    default void acceptBoolean(final boolean b) {
        acceptCell(b ? BooleanCell.TRUE : BooleanCell.FALSE);
    }
    @Override
    default void acceptBytes(final byte[] bytes) {
        DataCell cell;
        try {
            cell = new BinaryObjectCellFactory().create(bytes);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        acceptCell(cell);
    }
    @Override
    default void acceptString(final String str) {
        acceptCell(new StringCell(str));
    }
    @Override
    default void acceptNumber(final Number num) {
        acceptCell(new DoubleCell(num.doubleValue()));
    }
    @Override
    default void acceptInteger(final long value) {
        acceptCell(new LongCell(value));
    }
    @Override
    default void acceptFloat(final double value) {
        acceptCell(new DoubleCell(value));
    }
    @Override
    default void acceptList(final List<Object> list) {
        if (!list.isEmpty()) {
            for (final Object obj : list) {
                if (obj != null) {
                    acceptObject(obj);
                    return;
                }
            }
        }
        acceptNull();
    }
    @Override
    default void acceptMap(final Map<String, Object> map) {
        acceptCell(createJson(map));
    }
    @Override
    default void acceptNode(final Node node) {
        acceptMap(node.asMap());
    }
    @Override
    default void acceptRelationship(final Relationship rel) {
        acceptMap(rel.asMap());
    }
    @Override
    default void acceptPath(final Path path) {
        acceptString(String.valueOf(path));
    }
    @Override
    default void acceptPoint(final Point p) {
        final Map<String, Object> map = new HashMap<>();
        map.put("x", p.x());
        map.put("y", p.y());
        map.put("z", p.z());
        map.put("srid", p.srid());
        acceptMap(map);
    }
    @Override
    default void acceptDate(final LocalDate d) {
        acceptCell(LocalDateCellFactory.create(d));
    }
    @Override
    default void acceptLocalTime(final LocalTime time) {
        acceptCell(LocalTimeCellFactory.create(time));
    }
    @Override
    default void acceptLocalDateTime(final LocalDateTime time) {
        acceptCell(LocalDateTimeCellFactory.create(time));
    }
    @Override
    default void acceptDurationMilliseconds(final long duration) {
        acceptCell(DurationCellFactory.create(Duration.ofMillis(duration)));
    }
    @Override
    default void acceptNull() {
        acceptCell(new MissingCell("null"));
    }
    void acceptCell(DataCell type);
    @Override
    default void acceptUndefined(final Object value) {
        acceptCell(createJson(value));
    }
    default DataCell createJson(final Object value) {
        final StringWriter sw = new StringWriter();
        final JsonGenerator gen = Json.createGenerator(sw);

        final Neo4jValueWriter wr = new Neo4jValueWriter(gen, getConverter());
        wr.acceptObject(value);
        gen.flush();

        final String json = wr.toString();
        try {
            return JSONCellFactory.create(json, false);
        } catch (final IOException e) {
            return new MissingCell(e.getMessage());
        }
    }
}
