/**
 *
 */
package se.redfield.knime.neo4j.db;

import org.neo4j.driver.Value;
import org.neo4j.driver.types.TypeSystem;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4jDataConverter {
    //type system cache
    private final TypeSystem typeSystem;

    public Neo4jDataConverter(final TypeSystem s) {
        super();
        this.typeSystem = s;
    }

    public void convert(final Value v, final ConvertedValueConsumer c) {
        if (typeSystem.BOOLEAN().isTypeOf(v)) {
            c.acceptBoolean(v.asBoolean());
        } else if (typeSystem.BYTES().isTypeOf(v)) {
            c.acceptBytes(v.asByteArray());
        } else if (typeSystem.STRING().isTypeOf(v)) {
            c.acceptString(v.asString());
        } else if (typeSystem.INTEGER().isTypeOf(v)) {
            c.acceptInteger(v.asLong());
        } else if (typeSystem.FLOAT().isTypeOf(v)) {
            c.acceptFloat(v.asDouble());
        } else if (typeSystem.NUMBER().isTypeOf(v)) {
            c.acceptNumber(v.asNumber());
        } else if (typeSystem.LIST().isTypeOf(v)) {
            c.acceptList(v.asList());
        } else if (typeSystem.MAP().isTypeOf(v)) {
            c.acceptMap(v.asMap());
        } else if (typeSystem.NODE().isTypeOf(v)) {
            c.acceptNode(v.asNode());
        } else if (typeSystem.RELATIONSHIP().isTypeOf(v)) {
            c.acceptRelationship(v.asRelationship());
        } else if (typeSystem.PATH().isTypeOf(v)) {
            c.acceptPath(v.asPath());
        } else if (typeSystem.POINT().isTypeOf(v)) {
            c.acceptPoint(v.asPoint());
        } else if (typeSystem.DATE().isTypeOf(v)) {
            c.acceptDate(v.asLocalDate());
        } else if (typeSystem.TIME().isTypeOf(v)) {
            c.acceptTime(v.asOffsetTime());
        } else if (typeSystem.LOCAL_TIME().isTypeOf(v)) {
            c.acceptLocalTime(v.asLocalTime());
        } else if (typeSystem.LOCAL_DATE_TIME().isTypeOf(v)) {
            c.acceptLocalDateTime(v.asLocalDateTime());
        } else if (typeSystem.DATE_TIME().isTypeOf(v)) {
            c.acceptDateTime(v.asOffsetDateTime());
        } else if (typeSystem.DURATION().isTypeOf(v)) {
            c.acceptDuration(v.asIsoDuration());
        } else if (typeSystem.NULL().isTypeOf(v)) {
            c.acceptNull();
        }
    }
}
