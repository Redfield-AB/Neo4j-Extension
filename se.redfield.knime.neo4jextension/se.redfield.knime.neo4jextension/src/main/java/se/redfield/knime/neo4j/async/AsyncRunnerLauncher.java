/**
 *
 */
package se.redfield.knime.neo4j.async;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncRunnerLauncher<A, R> {
    private final boolean stopOnFailure;
    private final AsyncRunner<A, R> runner;
    private final NumberedSource<A> source;
    private final int numThreads;
    private final Consumer<R> consumer;
    private final boolean keepOrder;

    private final TreeSet<NumberedValue<R>> buffer = new TreeSet<>();
    private boolean hasErrors;
    private int nextIndex;

    public AsyncRunnerLauncher(final boolean stopOnFailure,
            final AsyncRunner<A, R> runner,
            final Iterator<A> source,
            final int numThreads,
            final Consumer<R> consumer,
            final boolean keepOrder,
            final int maxBufferSize) {
        super();
        this.stopOnFailure = stopOnFailure;
        this.runner = runner;
        this.numThreads = numThreads;
        this.consumer = consumer;
        this.keepOrder = keepOrder;
        this.source = new NumberedSource<A>(source) {
            @Override
            //just make synchronized
            public synchronized boolean hasNext() {
                return super.hasNext();
            }
            @Override
            public synchronized NumberedValue<A> next() {
                while (buffer.size() >= maxBufferSize) {
                    try {
                        wait();
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                return super.next();
            }
        };
    }

    /**
     * @param source
     * @param numThreads
     * @return
     */
    public void run() {
        if (!source.hasNext()) {
            return;
        }
        if (numThreads < 1) {
            throw new IllegalArgumentException("Number of threads " + numThreads + " < 1");
        }
        final int[] numThreadsHolder = {numThreads};

        synchronized (numThreadsHolder) {
            //start workers
            for (int i = 0; i < numThreads; i++) {
                createThread(numThreadsHolder, () -> runWorkerThread()).start();
            }
            //wait for finish of all threads
            while (numThreadsHolder[0] > 0) {
                try {
                    numThreadsHolder.wait();
                } catch (final InterruptedException e) {
                    hasErrors = true;
                }
            }
        }
    }

    private void runWorkerThread() {
        boolean isStarted = false;

        try {
            while (true) {
                NumberedValue<A> next;
                synchronized (source) {
                    if (!source.hasNext()) {
                        return;
                    }
                    try {
                        next = source.next();
                    } catch (final NoSuchElementException e) {
                        // it is possible when is launched by keep source order
                        // when the iterator waits of cleaning buffer
                       return;
                    }
                }
                if (!isStarted) {
                    isStarted = true;
                    workerStarted();
                }

                R result = null;
                try {
                    result = runner.run(next.getNumber(), next.getValue());
                } catch (final Throwable e) {
                    synchronized (source) {
                        hasErrors = true;
                        if (stopOnFailure) {
                            readToEnd();
                        }
                    }
                }

                addToOutput(new NumberedValue<>(next.getNumber(), result));
            }
        } finally {
            workerStopped();
        }
    }
    private void addToOutput(final NumberedValue<R> value) {
        if (consumer == null) {
            return;
        }

        final List<R> toFlush = new LinkedList<>();
        synchronized (buffer) {
            if (keepOrder) {
                synchronized (source) {
                    buffer.add(value);

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
            } else {
                consumer.accept(value.getValue());
            }
        }
    }

    private void workerStarted() {
        try {
            runner.workerStarted();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
    private void workerStopped() {
        try {
            runner.workerStopped();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
    private void readToEnd() {
        final Iterator<?> iter = source.getOriginIterator();
        while (iter.hasNext()) {
            iter.next();
        }
    }
    private Thread createThread(final int[] numThreads, final Runnable runnable) {
        return new Thread("Async runner thread") {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    synchronized (numThreads) {
                        numThreads[0]--;
                        numThreads.notify();
                    }
                }
            }
        };
    }

    /**
     * @return true if has errors.
     */
    public boolean hasErrors() {
        return hasErrors;
    }

    public static class Builder<A, R> {
        private AsyncRunner<A, R> runner;
        private Iterator<A> source;
        private int numThreads = 100;
        private boolean stopOnFailure;
        private Consumer<R> consumer;
        private boolean keepSourceOrder;
        private int maxBufferSize = -1;

        private Builder() {
            super();
        }
        private Builder(final AsyncRunner<A, R> runner) {
            super();
            this.runner = runner;
        }

        public static <A, R> Builder<A, R> newBuilder(final AsyncRunner<A, R> runner) {
            return new Builder<A, R>(runner);
        }
        public static <A, R> Builder<A, R> newBuilder() {
            return new Builder<A, R>();
        }
        public Builder<A, R> withRunner(final AsyncRunner<A, R> runner) {
            this.runner = runner;
            return this;
        }
        public Builder<A, R> withSource(final Iterator<A> source) {
            this.source = source;
            return this;
        }
        public Builder<A, R> withNumThreads(final int numTreads) {
            this.numThreads = numTreads;
            return this;
        }
        public Builder<A, R> withStopOnFailure(final boolean stopOnFailure) {
            this.stopOnFailure = stopOnFailure;
            return this;
        }
        public Builder<A, R> withConsumer(final Consumer<R> consumer) {
            this.consumer = consumer;
            return this;
        }
        public Builder<A, R> withKeepSourceOrder(final boolean keepSourceOrder) {
            this.keepSourceOrder = keepSourceOrder;
            return this;
        }
        public Builder<A, R> withMaxBufferSize(final int maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
            return this;
        }
        public AsyncRunnerLauncher<A, R> build() {
            testArguments();
            return new AsyncRunnerLauncher<>(
                    stopOnFailure,
                    runner,
                    source,
                    numThreads,
                    consumer,
                    keepSourceOrder,
                    maxBufferSize < 0 ? numThreads : maxBufferSize);
        }
        private void testArguments() {
            if (runner == null) {
                throw new NullPointerException("Async runner");
            }
            if (source == null) {
                throw new NullPointerException("Source iterator");
            }
            if (numThreads < 1) {
                throw new IllegalArgumentException("Number of threads should be greather tnan 1");
            }
            if (keepSourceOrder) {
                if (maxBufferSize == 0) {
                    throw new IllegalArgumentException(
                            "Max bugger size should be greater then zero when need to keep source order");
                }
                if (consumer == null) {
                    throw new IllegalArgumentException(
                            "Consumer should be specified when need to keep source order");
                }
            }
        }
    }
}
