/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AuthSchemeCellRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = -3791367144246741513L;

    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object originValue,
            final int index, final boolean isSelected, final boolean cellHasFocus) {
        Object value = originValue;
        if (value instanceof AuthScheme) {
            value = getAuthSchemeLabel((AuthScheme) originValue);
        }
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }

    private String getAuthSchemeLabel(final AuthScheme scheme) {
        switch (scheme) {
        case flowCredentials:
            return "credentials";
        default:
            return scheme.name();
        }
    }
}
