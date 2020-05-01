/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class ListClickListener<T> extends MouseAdapter {
    private final JList<T> list;
    private final ValueInsertHandler<T> handler;

    public ListClickListener(final JList<T> list, final ValueInsertHandler<T> h) {
        super();
        this.list = list;
        this.handler = h;
    }
    @Override
    public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
            final int index = list.getSelectedIndex();
            if (index > -1 && index == getUnderlyingElement(e.getX(), e.getY())) {
                final T value = list.getModel().getElementAt(index);
                if (handler != null) {
                    handler.insert(value);
                }
            }
        }
    }
    private int getUnderlyingElement(final int x, final int y) {
        final int size = list.getModel().getSize();
        for (int i = 0; i < size; i++) {
            final Rectangle b = list.getCellBounds(i, i);
            if (b.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }
}