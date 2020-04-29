/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class ListClickListener extends MouseAdapter {
    private final JList<String> list;
    private final ValueInsertHandler handler;

    private final int selectedIndex;;

    public ListClickListener(final JList<String> list, final ValueInsertHandler h, final int selectedIndex) {
        super();
        this.list = list;
        this.handler = h;
        this.selectedIndex = selectedIndex;
    }
    @Override
    public void mouseClicked(final MouseEvent e) {
        if (selectedIndex == list.getSelectedIndex()) {
            final String value = list.getModel().getElementAt(list.getSelectedIndex());
            if (handler != null) {
                handler.insert(value);
            }
        }
    }
}
