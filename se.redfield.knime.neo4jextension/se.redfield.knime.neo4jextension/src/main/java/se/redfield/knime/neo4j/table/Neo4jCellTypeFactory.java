/**
 *
 */
package se.redfield.knime.neo4j.table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedList;
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
 * Warning: Not thread safe
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public abstract class Neo4jCellTypeFactory implements ConvertedValueConsumer {
    private List<DataType> currentListTypes;

    @Override
    public void acceptBoolean(final boolean b) {
        acceptCellTypeInternal(BooleanCell.TYPE);
    }
    @Override
    public void acceptBytes(final byte[] bytes) {
        acceptCellTypeInternal(BinaryObjectDataCell.TYPE);
    }
    @Override
    public void acceptString(final String str) {
        acceptCellTypeInternal(StringCell.TYPE);
    }
    @Override
    public void acceptNumber(final Number num) {
        acceptCellTypeInternal(DoubleCell.TYPE);
    }
    @Override
    public void acceptInteger(final long value) {
        acceptCellTypeInternal(LongCell.TYPE);
    }
    @Override
    public void acceptFloat(final double value) {
        acceptCellTypeInternal(DoubleCell.TYPE);
    }
    @Override
    public void acceptList(final List<Object> list) {
        if (isInList()) {
            acceptCellTypeInternal(JSONCell.TYPE);
            return;
        }
        currentListTypes = new LinkedList<>();
        if (!list.isEmpty()) {
            for (final Object obj : list) {
                if (obj != null) {
                    acceptObject(obj);
                    break;
                }
            }
        }
        acceptCellType(new DataTypeDetection(true, currentListTypes));
        currentListTypes = null;
    }
    @Override
    public void acceptMap(final Map<String, Object> map) {
        acceptCellTypeInternal(JSONCell.TYPE);
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
        acceptCellTypeInternal(StringCell.TYPE);
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
        acceptCellTypeInternal(LocalDateCellFactory.TYPE);
    }
    @Override
    public void acceptLocalTime(final LocalTime time) {
        acceptCellTypeInternal(LocalTimeCellFactory.TYPE);
    }
    @Override
    public void acceptLocalDateTime(final LocalDateTime time) {
        acceptCellTypeInternal(LocalDateTimeCellFactory.TYPE);
    }
    @Override
    public void acceptDuration(final IsoDuration d) {
        acceptCellTypeInternal(DurationCellFactory.TYPE);
    }
    @Override
    public void acceptDurationMilliseconds(final long duration) {
        throw new RuntimeException("use method acceptDuration(IsoDuration d) instead");
    }
    @Override
    public void acceptNull() {
        acceptCellTypeInternal(null);
    }
    private void acceptCellTypeInternal(final DataType type) {
        if (isInList()) {
            if (type != null) {
                currentListTypes.add(type);
            }
        } else {
            acceptCellType(new DataTypeDetection(type));
        }
    }
    private boolean isInList() {
        return currentListTypes != null;
    }
    protected abstract void acceptCellType(DataTypeDetection detection);
    @Override
    public void acceptUndefined(final Object value) {
        acceptString(String.valueOf(value));
    }
}
