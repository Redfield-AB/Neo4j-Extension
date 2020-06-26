/**
 *
 */
package se.redfield.knime.neo4j.async;

import java.util.Iterator;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class IteratorProxy<E> implements Iterator<E> {
    private Iterator<E> iterator;

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }
    @Override
    public E next() {
        return iterator.next();
    }
    public void setIterator(final Iterator<E> iterator) {
        this.iterator = iterator;
    }
    public Iterator<E> getIterator() {
        return iterator;
    }
}
