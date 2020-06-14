/**
 *
 */
package se.redfield.knime.neo4j.writer;

import java.util.LinkedList;

import se.redfield.knime.neo4j.async.NumberedValue;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public abstract class OrderedDataConsumer<T> {
    private long nextNumber = 0;
    private int maxQueueSize = 20;
    private final LinkedList<NumberedValue<T>> queue = new LinkedList<>();

    /**
     * Default constructor.
     */
    public OrderedDataConsumer() {
        super();
    }

    public void add(final long index, final T value) {
        synchronized (queue) {
            queue.notify();
            while (true) {
                final boolean triggered = index == nextNumber;
                if (triggered) {
                    addToOutputInternal(value);

                    //if triggered, attempt to clear queue.
                    while (!queue.isEmpty()) {
                        final NumberedValue<T> first = queue.getFirst();
                        if (first.getNumber() != nextNumber) {
                            return;
                        }
                        addToOutputInternal(value);
                    }

                    return;
                }

                //if is possible just add to queue and return
                if (queue.size() < getMaxQueueSize()) {
                    queue.add(new NumberedValue<>(index, value));
                    return;
                }

                //if not possible to add to queue
                //need to wait when will possible
                try {
                    queue.wait();
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * @param value
     */
    private void addToOutputInternal(final T value) {
        addToOutput(value);
        nextNumber++;
    }

    /**
     * @param value
     */
    protected abstract void addToOutput(T value);

    /**
     * @return max queue size.
     */
    public int getMaxQueueSize() {
        return maxQueueSize;
    }
    /**
     * @param size max queue size.
     */
    public void setMaxQueueSize(final int size) {
        this.maxQueueSize = size;
    }
}
