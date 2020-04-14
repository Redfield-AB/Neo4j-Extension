/**
 *
 */
package se.redfield.knime.neo4jextension;

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
import org.neo4j.driver.types.TypeSystem;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class JsonBuilder {
    private final TypeSystem types;
    public JsonBuilder(final TypeSystem types) {
        super();
        this.types = types;
    }

    public void writeJson(final List<Record> records, final JsonGenerator gen) {
        gen.writeStartArray();
        for (final Record r : records) {
            writeRecord(r, gen);
        }
        gen.writeEnd();
    }
    private void writeRecord(final Record r, final JsonGenerator gen) {
        final int size = r.size();
        gen.writeStartArray();
        for (int i = 0; i < size; i++) {
            writeValue(r.get(i), gen);
        }
        gen.writeEnd();
    }
    private void writeValue(final Value value, final JsonGenerator gen) {
        if (types.BOOLEAN().isTypeOf(value)) {
            writeBoolean(value.asBoolean(), gen);
        } else if (types.BYTES().isTypeOf(value)) {
            writeString("[byte array]", gen);
        } else if (types.STRING().isTypeOf(value)) {
            writeString(value.asString(), gen);
        } else if (types.NUMBER().isTypeOf(value)) {
            writeDouble(value.asDouble(), gen);
        } else if (types.INTEGER().isTypeOf(value)) {
            writeInt(value.asInt(), gen);
        } else if (types.FLOAT().isTypeOf(value)) {
            writeFloat(value.asFloat(), gen);
        } else if (types.LIST().isTypeOf(value)) {
            writeList(value.asList(), gen);
        } else if (types.MAP().isTypeOf(value)) {
            writeMap(value.asMap(), gen);
        } else if (types.NODE().isTypeOf(value)) {
            writeNode(value.asNode(), gen);
        } else if (types.RELATIONSHIP().isTypeOf(value)) {
            writeRelationship(value.asRelationship(), gen);
        } else if (types.PATH().isTypeOf(value)) {
            writePath(value.asPath(), gen);
        } else if (types.POINT().isTypeOf(value)) {
            writePoint(value.asPoint(), gen);
        } else if (types.DATE().isTypeOf(value)) {
            writeDate(value.asLocalDate(), gen);
        } else if (types.TIME().isTypeOf(value)) {
            writeTime(value.asLocalTime(), gen);
        } else if (types.LOCAL_TIME().isTypeOf(value)) {
            writeTime(value.asLocalTime(), gen);
        } else if (types.LOCAL_DATE_TIME().isTypeOf(value)) {
            writeDate(value.asLocalDate(), gen);
        } else if (types.DATE_TIME().isTypeOf(value)) {
            writeDateTime(value.asLocalDateTime(), gen);
        } else if (types.DURATION().isTypeOf(value)) {
            writeString(value.asString(), gen);
        } else if (types.NULL().isTypeOf(value)) {
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
