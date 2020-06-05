/**
 *
 */
package se.redfield.knime.neo4j.table;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.neo4j.driver.Value;

import se.redfield.knime.neo4j.db.Neo4jDataConverter;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4jTableOutputSupport {
    private final Neo4jDataConverter converter;

    public Neo4jTableOutputSupport(final Neo4jDataConverter conv) {
        super();
        this.converter = conv;
    }

    /**
     * @param v value.
     * @return compatible data type.
     */
    public DataType getCompatibleCellType(final Value v) {
        final DataType[] ref = {null};
        final Neo4jCellTypeFactory f = new Neo4jCellTypeFactory() {
            @Override
            public void acceptCellType(final DataType type) {
                ref[0] = type;
            }
            @Override
            public Neo4jDataConverter getConverter() {
                return converter;
            }
        };
        converter.convert(v, f);
        final DataType type = ref[0];
        return type == null ? StringCell.TYPE : type;
    }
    /**
     * @param value value.
     * @return compatible cell.
     */
    public DataCell createCell(final Value value) {
        final DataCell[] ref = {null};
        final Neo4jCellFactory f = new Neo4jCellFactory() {
            @Override
            public Neo4jDataConverter getConverter() {
                return converter;
            }
            @Override
            public void acceptCell(final DataCell type) {
                ref[0] = type;
            }
        };
        converter.convert(value, f);
        return ref[0];
    }
}
