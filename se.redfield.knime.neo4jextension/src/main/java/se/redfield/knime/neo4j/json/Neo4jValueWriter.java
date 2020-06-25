/**
 *
 */
package se.redfield.knime.neo4j.json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.json.stream.JsonGenerator;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Path.Segment;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;

import se.redfield.knime.neo4j.db.ConvertedValueConsumer;
import se.redfield.knime.neo4j.db.Neo4jDataConverter;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4jValueWriter implements ConvertedValueConsumer {
    private final JsonGenerator gen;
    private final Neo4jDataConverter converter;

    public Neo4jValueWriter(final JsonGenerator out, final Neo4jDataConverter adapter) {
        super();
        this.gen = out;
        this.converter = adapter;
    }

    @Override
    public void acceptBoolean(final boolean b) {
        gen.write(b);
    }
    @Override
    public void acceptBytes(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (final byte b : bytes) {
            final String hex = Integer.toHexString(0xFF & b);
            if (hex.length() < 2) {
                sb.append('0');
            }
            sb.append(hex);
        }
        acceptString(sb.toString());
    }
    @Override
    public void acceptString(final String str) {
        gen.write(str);
    }
    @Override
    public void acceptNumber(final Number num) {
        gen.write(num.doubleValue());
    }
    @Override
    public void acceptInteger(final long value) {
        gen.write(value);
    }
    @Override
    public void acceptFloat(final double value) {
        gen.write(value);
    }

    @Override
    public void acceptList(final List<Object> values) {
        gen.writeStartArray();
        for (final Object value : values) {
            acceptObject(value);
        }
        gen.writeEnd();
    }

    @Override
    public void acceptMap(final Map<String, Object> map) {
        gen.writeStartObject();
        for (final Map.Entry<String, Object> e : map.entrySet()) {
            final String key = e.getKey();
            final Object value = e.getValue();

            gen.writeKey(key);
            acceptObject(value);
        }
        gen.writeEnd();
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
        gen.writeStartArray();
        for (final Segment seg : path) {
            gen.write(seg.toString());
        }
        gen.writeEnd();
    }
    @Override
    public void acceptPoint(final Point p) {
        gen.writeStartObject();

        gen.write("srid", p.srid());
        gen.write("x", p.x());
        gen.write("y", p.y());
        gen.write("z", p.z());

        gen.writeEnd();
    }
    @Override
    public void acceptDate(final LocalDate d) {
        gen.write(d.format(DateTimeFormatter.ISO_DATE));
    }
    @Override
    public void acceptLocalTime(final LocalTime time) {
        gen.write(time.format(DateTimeFormatter.ISO_TIME));
    }
    @Override
    public void acceptLocalDateTime(final LocalDateTime time) {
        gen.write(time.format(DateTimeFormatter.ISO_DATE_TIME));
    }
    @Override
    public void acceptDurationMilliseconds(final long duration) {
        gen.write(duration);
    }
    @Override
    public void acceptNull() {
        gen.writeNull();
    }
    @Override
    public Neo4jDataConverter getConverter() {
        return this.converter;
    }
}
