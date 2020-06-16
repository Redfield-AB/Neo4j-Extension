/**
 *
 */
package se.redfield.knime.neo4j.async;

import java.util.Iterator;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncRunnerLauncher<A> {
    private boolean stopOnFailure;
    private boolean hasErrors;
    private AsyncRunner<A> runner;
    private final Iterator<NumberedValue<A>> source;
    private Object sync;
    private int numThreads;

    private AsyncRunnerLauncher(final AsyncRunner<A> runner, final Iterator<A> iterator,
            final int numThreads) {
        this(iterator, numThreads);
        this.runner = runner;
    }
    //just for unit tests
    protected AsyncRunnerLauncher(final Iterator<A> iterator, final int numThreads) {
        super();
        this.source = new NumberedIterator<A>(iterator);
        setSyncObject(iterator);
        this.numThreads = numThreads;
    }
    /**
     * @param syncObject object for synchronization.
     */
    public void setSyncObject(final Object syncObject) {
        this.sync = syncObject;
    }
    protected void setRunner(final AsyncRunner<A> runner) {
        this.runner = runner;
    }

    public void setStopOnFailure(final boolean stop) {
        this.stopOnFailure = stop;
    }
    public boolean isStopOnFailure() {
        return stopOnFailure;
    }
    public int getNumThreads() {
        return numThreads;
    }
    public void setNumThreads(final int numThreads) {
        this.numThreads = numThreads;
    }

    /**
     * @param runner runner.
     * @param arguments list of arguments to execute.
     * @param numThreads number of threads.
     * @return map of execution results to its position in origin list.
     */
    public static <A> AsyncRunnerLauncher<A> createLauncher(final AsyncRunner<A> runner,
            final Iterator<A> arguments, final int numThreads) {
        return new AsyncRunnerLauncher<>(runner, arguments, numThreads);
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
                synchronized (sync) {
                    if (!source.hasNext()) {
                        return;
                    }
                    next = source.next();
                }
                if (!isStarted) {
                    isStarted = true;
                    workerStarted();
                }

                try {
                    runner.run(next.getNumber(), next.getValue());
                } catch (final Throwable e) {
                    synchronized (sync) {
                        hasErrors = true;
                        if (isStopOnFailure()) {
                            readToEnd(source);
                        }
                    }
                }
            }
        } finally {
            workerStopped();
        }
    }
    /**
     */
    private void workerStarted() {
        try {
            runner.workerStarted();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
    /**
     */
    private void workerStopped() {
        try {
            runner.workerStopped();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    private void readToEnd(final Iterator<NumberedValue<A>> iter) {
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
}
