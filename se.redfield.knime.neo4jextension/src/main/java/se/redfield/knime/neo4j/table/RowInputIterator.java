/**
 *
 */
package se.redfield.knime.neo4j.table;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.knime.core.data.DataRow;
import org.knime.core.node.streamable.RowInput;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class RowInputIterator implements Iterator<DataRow> {
    private final RowInput input;
    private DataRow next;

    public RowInputIterator(final RowInput input) {
        super();
        this.input = input;
    }

    @Override
    public boolean hasNext() {
        if (next == null) {
            try {
                next = input.poll();
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return next != null;
    }
    @Override
    public DataRow next() {
        if (hasNext()) {
            final DataRow tmp = next;
            next = null;
            return tmp;
        }
        throw new NoSuchElementException();
    }
}
