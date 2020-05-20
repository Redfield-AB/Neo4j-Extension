/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.knime.core.node.util.SharedIcons;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class SourceColumnCellRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 4996318397629191499L;

    /**
     * Default constructor.
     */
    public SourceColumnCellRenderer() {
        super();
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends Object> list,
            final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
        final Component comp = super.getListCellRendererComponent(list,
                value, index, isSelected, cellHasFocus);
        setIcon(SharedIcons.TYPE_STRING.get());
        return comp;
    }
}
