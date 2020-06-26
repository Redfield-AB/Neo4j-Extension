/**
 *
 */
package se.redfield.knime.neo4j.db;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Value;
import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public interface ConvertedValueConsumer {
    void acceptBoolean(boolean b);
    void acceptBytes(byte[] bytes);
    void acceptString(String str);
    void acceptNumber(Number num);
    void acceptInteger(long value);
    void acceptFloat(double value);
    void acceptList(List<Object> values);
    void acceptMap(Map<String, Object> map);
    void acceptNode(Node node);
    void acceptRelationship(Relationship rel);
    void acceptPath(Path path);
    void acceptPoint(Point p);
    void acceptDate(LocalDate d);
    default void acceptTime(final OffsetTime time) {
        final LocalTime t = time == null ? null : time.toLocalTime();
        acceptLocalTime(t);
    }
    void acceptLocalTime(LocalTime time);
    void acceptLocalDateTime(LocalDateTime time);
    default void acceptDateTime(final OffsetDateTime time) {
        final LocalDateTime t = time == null ? null : time.toLocalDateTime();
        acceptLocalDateTime(t);
    }
    default void acceptDuration(final IsoDuration d) {
        long millis = d.seconds() * 1000l;
        millis += d.nanoseconds() / 1000000l;
        acceptDurationMilliseconds(millis);
    }
    void acceptDurationMilliseconds(long duration);
    void acceptNull();

    @SuppressWarnings("unchecked")
    default void acceptObject(final Object value) {
        if (value instanceof Value) {
            getConverter().convert((Value) value, this);
        } else {
            if (value == null) {
                acceptNull();
            }
            //number
            else if (value instanceof BigInteger
                    || value instanceof Byte
                    || value instanceof Integer
                    || value instanceof Long
                    || value instanceof Short) {
                acceptInteger(((Number) value).longValue());
            } else if (value instanceof Number) {
                acceptNumber((Number) value);
            } else if (value instanceof String) {
                acceptString((String) value);
            } else if (value instanceof List) {
                acceptList((List<Object>) value);
            } else if (value instanceof Map) {
                acceptMap((Map<String, Object>) value);
            } else if (value instanceof Node) {
                acceptNode((Node) value);
            } else if (value instanceof Relationship) {
                acceptRelationship((Relationship) value);
            } else if (value instanceof Path) {
                acceptPath((Path) value);
            } else if (value instanceof Point) {
                acceptPoint((Point) value);
            } else if (value instanceof LocalDate) {
                acceptDate((LocalDate) value);
            } else if (value instanceof OffsetTime) {
                acceptTime((OffsetTime) value);
            } else if (value instanceof LocalTime) {
                acceptLocalTime((LocalTime) value);
            } else if (value instanceof LocalDateTime) {
                acceptLocalDateTime((LocalDateTime) value);
            } else if (value instanceof OffsetDateTime) {
                acceptDateTime((OffsetDateTime) value);
            } else if (value instanceof IsoDuration) {
                acceptDuration((IsoDuration) value);
            } else if (value instanceof Date) {
                final LocalDateTime ldt = ((Date) value).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
                acceptLocalDateTime(ldt);
            } else {
                acceptUndefined(value);
            }
        }
    }
    default void acceptUndefined(final Object value) {
        //by default undefined value is handled as the string
        acceptString(String.valueOf(value));
    }
    Neo4jDataConverter getConverter();
}
