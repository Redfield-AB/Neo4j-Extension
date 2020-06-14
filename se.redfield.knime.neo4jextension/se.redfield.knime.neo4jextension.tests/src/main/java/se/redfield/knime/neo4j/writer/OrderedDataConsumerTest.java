/**
 *
 */
package se.redfield.knime.neo4j.writer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.AssertionFailedError;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class OrderedDataConsumerTest extends OrderedDataConsumer<Integer> {
    private int currentValue;
    private List<Integer> errors = new LinkedList<>();

    /**
     * Default constructor.
     */
    public OrderedDataConsumerTest() {
        super();
    }

    @Test
    public void statisticalTest() {
        final AtomicInteger max = new AtomicInteger(1000000);
        currentValue = max.get() + 1;
        setMaxQueueSize(17);

        final AtomicBoolean mutex = new AtomicBoolean();
        final AtomicInteger numThreads = new AtomicInteger(30);

        for (int i = 0; i < 30; i++) {
            new Thread() {
                @Override
                public void run() {
                    synchronized (mutex) {
                        try {
                            mutex.wait();
                        } catch (final InterruptedException e) {
                        }
                    }

                    while (true) {
                        synchronized (mutex) {
                            if (max.get() > 0) {
                                addToOutput(max.get());
                                max.addAndGet(-1);
                            } else {
                                break;
                            }
                        }
                        Thread.yield();
                    }

                    synchronized(mutex) {
                        numThreads.addAndGet(-1);
                        mutex.notify();
                    }
                }
            }.start();
        }

        try {
            Thread.sleep(5000l);
        } catch (final InterruptedException e1) {
        }

        synchronized (mutex) {
            mutex.notifyAll();
            while (numThreads.get() > 0) {
                try {
                    mutex.wait();
                } catch (final InterruptedException e) {
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new AssertionFailedError("Have errors: " + errors);
        }
    }

    @Override
    protected void addToOutput(final Integer value) {
        if (currentValue - value != 1) {
            System.out.println("Current value: " + currentValue + ", value: " + value);
            errors.add(value);
        }
        currentValue = value;
        System.out.println("Added by " + Thread.currentThread().getId() + ": " + currentValue);
    }
}
