/**
 *
 */
package se.redfield.knime.neo4j.async;

import java.util.Iterator;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class NumberedIterator<E> implements Iterator<NumberedValue<E>> {
    private final Iterator<E> iter;
    private int counter;

    /**
     * @param iter iterator.
     */
    public NumberedIterator(final Iterator<E> iter) {
        super();
        this.iter = iter;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public NumberedValue<E> next() {
        final NumberedValue<E> value = new NumberedValue<E>(counter, iter.next());
        counter++;
        return value;
    }
}
