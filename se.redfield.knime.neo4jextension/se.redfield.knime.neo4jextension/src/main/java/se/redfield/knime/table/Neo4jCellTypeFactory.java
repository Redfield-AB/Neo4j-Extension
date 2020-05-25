/**
 *
 */
package se.redfield.knime.table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;

import se.redfield.knime.neo4j.db.ConvertedValueConsumer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public interface Neo4jCellTypeFactory extends ConvertedValueConsumer {
    @Override
    default void acceptBoolean(final boolean b) {
        acceptCellType(BooleanCell.TYPE);
    }
    @Override
    default void acceptBytes(final byte[] bytes) {
        acceptCellType(BinaryObjectDataCell.TYPE);
    }
    @Override
    default void acceptString(final String str) {
        acceptCellType( StringCell.TYPE);
    }
    @Override
    default void acceptNumber(final Number num) {
        acceptCellType(DoubleCell.TYPE);
    }
    @Override
    default void acceptInteger(final long value) {
        acceptCellType(LongCell.TYPE);
    }
    @Override
    default void acceptFloat(final double value) {
        acceptCellType(DoubleCell.TYPE);
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
        acceptCellType(JSONCell.TYPE);
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
        acceptCellType(StringCell.TYPE);
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
        acceptCellType(LocalDateCellFactory.TYPE);
    }
    @Override
    default void acceptLocalTime(final LocalTime time) {
        acceptCellType(LocalTimeCellFactory.TYPE);
    }
    @Override
    default void acceptLocalDateTime(final LocalDateTime time) {
        acceptCellType(LocalDateTimeCellFactory.TYPE);
    }
    @Override
    default void acceptDuration(final IsoDuration d) {
        acceptCellType(DurationCellFactory.TYPE);
    }
    @Override
    default void acceptDurationMilliseconds(final long duration) {
        throw new RuntimeException("use method acceptDuration(IsoDuration d) instead");
    }
    @Override
    default void acceptNull() {
        acceptCellType(null);
    }
    void acceptCellType(DataType type);
    @Override
    default void acceptUndefined(final Object value) {
        acceptCellType(JSONCell.TYPE);
    }
}
