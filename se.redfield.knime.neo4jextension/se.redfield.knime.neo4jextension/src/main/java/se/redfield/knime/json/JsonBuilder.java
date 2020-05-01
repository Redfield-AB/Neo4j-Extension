/**
 *
 */
package se.redfield.knime.json;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.json.stream.JsonGenerator;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Path.Segment;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;

import se.redfield.knime.neo4j.db.DataAdapter;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class JsonBuilder {
    private final DataAdapter adapter;
    public JsonBuilder(final DataAdapter types) {
        super();
        this.adapter = types;
    }

    public void writeJson(final List<Record> records, final JsonGenerator gen) {
        gen.writeStartArray();
        for (final Record r : records) {
            writeRecord(r, gen);
        }
        gen.writeEnd();
    }
    private void writeRecord(final Record r, final JsonGenerator gen) {
        gen.writeStartObject();
        for (final String key: r.keys()) {
            gen.writeKey(key);
            writeValue(r.get(key), gen);
        }
        gen.writeEnd();
    }
    private void writeValue(final Value value, final JsonGenerator gen) {
        if (adapter.isBoolean(value)) {
            writeBoolean(value.asBoolean(), gen);
        } else if (adapter.isBytes(value)) {
            writeString("[byte array]", gen);
        } else if (adapter.isString(value)) {
            writeString(value.asString(), gen);
        } else if (adapter.isInteger(value)) {
            writeInt(value.asInt(), gen);
        } else if (adapter.isFloat(value)) {
            writeFloat(value.asFloat(), gen);
        } else if (adapter.isNumber(value)) {
            writeDouble(value.asDouble(), gen);
        } else if (adapter.isList(value)) {
            writeList(value.asList(), gen);
        } else if (adapter.isMap(value)) {
            writeMap(value.asMap(), gen);
        } else if (adapter.isNode(value)) {
            writeNode(value.asNode(), gen);
        } else if (adapter.isRelationship(value)) {
            writeRelationship(value.asRelationship(), gen);
        } else if (adapter.isPath(value)) {
            writePath(value.asPath(), gen);
        } else if (adapter.isPoint(value)) {
            writePoint(value.asPoint(), gen);
        } else if (adapter.isDate(value)) {
            writeDate(value.asLocalDate(), gen);
        } else if (adapter.isTime(value)) {
            writeTime(value.asLocalTime(), gen);
        } else if (adapter.islocalTime(value)) {
            writeTime(value.asLocalTime(), gen);
        } else if (adapter.isLocalDateTime(value)) {
            writeDate(value.asLocalDate(), gen);
        } else if (adapter.isDateTime(value)) {
            writeDateTime(value.asLocalDateTime(), gen);
        } else if (adapter.isDuration(value)) {
            writeString(value.asString(), gen);
        } else if (adapter.isNull(value)) {
            writeNull(gen);
        }
    }

    private void writeNull(final JsonGenerator gen) {
        gen.writeNull();
    }
    private void writeDateTime(final LocalDateTime time, final JsonGenerator gen) {
        gen.write(time.format(DateTimeFormatter.ISO_DATE_TIME));
    }
    private void writeTime(final LocalTime time, final JsonGenerator gen) {
        gen.write(time.format(DateTimeFormatter.ISO_TIME));
    }
    private void writeDate(final LocalDate time, final JsonGenerator gen) {
        gen.write(time.format(DateTimeFormatter.ISO_DATE));
    }
    private void writePoint(final Point p, final JsonGenerator gen) {
        gen.writeStartObject();

        gen.write("srid", p.srid());
        gen.write("x", p.x());
        gen.write("y", p.y());
        gen.write("z", p.z());

        gen.writeEnd();
    }
    private void writePath(final Path p, final JsonGenerator gen) {
        gen.writeStartArray();
        for (final Segment seg : p) {
            gen.write(seg.toString());
        }
        gen.writeEnd();
    }
    private void writeRelationship(final Relationship r, final JsonGenerator gen) {
        gen.write(r.toString());
    }
    private void writeNode(final Node n, final JsonGenerator gen) {
        gen.write(n.toString());
    }
    private void writeMap(final Map<String, Object> map, final JsonGenerator gen) {
        gen.writeStartObject();
        for (final Map.Entry<String, Object> e : map.entrySet()) {
            final String key = e.getKey();
            final Object value = e.getValue();

            if (value instanceof BigInteger
                    || value instanceof Byte
                    || value instanceof Integer
                    || value instanceof Long
                    || value instanceof Short) {
                gen.write(key, ((Number) value).longValue());
            } else if (value instanceof Number) {
                gen.write(key, ((Number) value).doubleValue());
            } else {
                gen.write(key, String.valueOf(value));
            }
        }
        gen.writeEnd();
    }

    private void writeList(final List<Object> list, final JsonGenerator gen) {
        gen.writeStartArray();
        for (final Object value : list) {
            if (value instanceof BigInteger
                    || value instanceof Byte
                    || value instanceof Integer
                    || value instanceof Long
                    || value instanceof Short) {
                gen.write(((Number) value).longValue());
            } else if (value instanceof Number) {
                gen.write(((Number) value).doubleValue());
            } else {
                gen.write(String.valueOf(value));
            }
        }
        gen.writeEnd();
    }
    private void writeFloat(final float f, final JsonGenerator gen) {
        gen.write(f);
    }
    private void writeInt(final int i, final JsonGenerator gen) {
        gen.write(i);
    }
    private void writeDouble(final double d, final JsonGenerator gen) {
        gen.write(d);
    }
    private void writeBoolean(final boolean b, final JsonGenerator gen) {
        gen.write(b);
    }
    private void writeString(final String str, final JsonGenerator gen) {
        gen.write(str);
    }
}
