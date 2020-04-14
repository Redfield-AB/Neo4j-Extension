/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.util.Objects;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;

import se.redfield.knime.neo4jextension.cfg.ConnectorConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorCell extends DataCell {
    private static final long serialVersionUID = 4770443096742507498L;

    public static final DataType TYPE = DataType.getType(ConnectorCell.class);

    private final ConnectorConfigSerializer connector;

    public ConnectorCell(final ConnectorConfigSerializer connector) {
        super();
        this.connector = connector;
    }


    @Override
    public String toString() {
        return connector.toString();
    }

    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if (!(dc instanceof ConnectorCell)) {
            return false;
        }

        final ConnectorCell that = (ConnectorCell) dc;
        return Objects.equals(connector, that.connector);
    }

    @Override
    public int hashCode() {
        return connector.hashCode();
    }
}
