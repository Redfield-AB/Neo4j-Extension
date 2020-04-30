/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import se.redfield.knime.neo4j.connector.NamedWithProperties;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class NamedWithPropertiesRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 5223006886411453577L;

    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object origin, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        final Object value = origin instanceof NamedWithProperties
                ? ((NamedWithProperties) origin).getName() : origin;
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
}
