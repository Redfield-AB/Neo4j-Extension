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

    public AsyncRunnerLauncher(final AsyncRunner<A> runner) {
        super();
        this.runner = runner;
    }
    //just for unit tests
    protected AsyncRunnerLauncher() {
        super();
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

    /**
     * @param arguments list of arguments to execute.
     * @param numThreads number of threads.
     * @return map of execution results to its position in origin list.
     */
    public void run(final Iterator<A> arguments, final int numThreads) {
        if (!arguments.hasNext()) {
            return;
        }
        if (numThreads < 1) {
            throw new IllegalArgumentException("Number of threads " + numThreads + " < 1");
        }
        doRun(new NumberedIterator<A>(arguments), numThreads);
    }
    /**
     * @param iter
     * @param numThreads
     * @return
     */
    private void doRun(final Iterator<NumberedValue<A>> iter, final int numThreads) {
        final int[] numThreadsHolder = {numThreads};

        synchronized (numThreadsHolder) {
            //start workers
            for (int i = 0; i < numThreads; i++) {
                createThread(numThreadsHolder, () -> runScripts(iter)).start();
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

    private void runScripts(final Iterator<NumberedValue<A>> source) {
        boolean isStarted = false;

        try {
            while (true) {
                NumberedValue<A> next;
                synchronized (source) {
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
                    synchronized (source) {
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
