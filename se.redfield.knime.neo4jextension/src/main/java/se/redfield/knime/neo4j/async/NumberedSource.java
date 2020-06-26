/**
 *
 */
package se.redfield.knime.neo4j.async;

import java.util.Iterator;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class NumberedSource<E> implements Iterator<NumberedValue<E>> {
    protected final Iterator<E> iterator;
    private int counter;

    /**
     * @param iter iterator.
     */
    public NumberedSource(final Iterator<E> iter) {
        super();
        this.iterator = iter;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }
    @Override
    public NumberedValue<E> next() {
        final NumberedValue<E> value = new NumberedValue<E>(counter, iterator.next());
        counter++;
        return value;
    }
    public Iterator<E> getOriginIterator() {
        return iterator;
    }
}
