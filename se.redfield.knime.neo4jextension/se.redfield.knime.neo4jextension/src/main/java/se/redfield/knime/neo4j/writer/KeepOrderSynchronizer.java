/**
 *
 */
package se.redfield.knime.neo4j.writer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

import se.redfield.knime.neo4j.async.NumberedValue;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class KeepOrderSynchronizer<S, R> {
    private final Consumer<R> consumer;
    private final Iterator<S> source;
    private int maxBufferSize = 20;
    private TreeSet<NumberedValue<R>> buffer = new TreeSet<>();
    private int nextIndex;

    /**
     * @param consumer consumer.
     */
    public KeepOrderSynchronizer(final Consumer<R> consumer, final Iterator<S> source) {
        super();
        this.source = new Iterator<S>() {
            @Override
            public synchronized boolean hasNext() {
                return source.hasNext();
            }
            @Override
            public synchronized S next() {
                while (buffer.size() >= getMaxBufferSize()) {
                    try {
                        wait();
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                return source.next();
            }
        };
        this.consumer = consumer;
        if (source == null) {
            throw new NullPointerException("Source");
        }
        if (consumer == null) {
            throw new NullPointerException("Consumer");
        }
    }
    public Iterator<S> getSynchronizedIterator() {
        return source;
    }
    public void addToOutput(final long num, final R value) {
        final List<R> toFlush = new LinkedList<>();
        synchronized (buffer) {
            synchronized (source) {
                buffer.add(new NumberedValue<>(num, value));

                while (!buffer.isEmpty()) {
                    if (buffer.first().getNumber() != nextIndex) {
                        break;
                    }
                    toFlush.add(buffer.pollFirst().getValue());
                    nextIndex++;
                }

                if (!toFlush.isEmpty()) {
                    synchronized(source) {
                        source.notifyAll();
                    }
                }
            }

            for (final R r : toFlush) {
                try {
                    consumer.accept(r);
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int getMaxBufferSize() {
        return maxBufferSize;
    }
    public void setMaxBufferSize(final int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Buffer size should be 1 or greater");
        }
        this.maxBufferSize = size;
    }
}
