/**
 *
 */
package se.redfield.knime.neo4j.async;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncRunnerLauncher<R, A> {
    private boolean stopOnFailure;
    private boolean hasErrors;
    private AsyncRunner<R, A> runner;

    public AsyncRunnerLauncher(final AsyncRunner<R, A> runner) {
        super();
        this.runner = runner;
    }
    //just for unit tests
    protected AsyncRunnerLauncher() {
        super();
    }
    protected void setRunner(final AsyncRunner<R, A> runner) {
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
    public Map<Long, R> run(final Iterator<A> arguments, final int numThreads) {
        if (!arguments.hasNext()) {
            return new HashMap<>();
        }
        if (numThreads < 1) {
            throw new IllegalArgumentException("Number of threads " + numThreads + " < 1");
        }
        return doRun(new NumberedIterator<A>(arguments), numThreads);
    }
    /**
     * @param iter
     * @param numThreads
     * @return
     */
    private Map<Long, R> doRun(final Iterator<NumberedValue<A>> iter, final int numThreads) {
        final int[] numThreadsHolder = {numThreads};
        final Map<Long, R> result = new HashMap<>();

        synchronized (numThreadsHolder) {
            //start workers
            for (int i = 0; i < numThreads; i++) {
                createThread(numThreadsHolder, () -> runScripts(iter, result)).start();
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
        return result;
    }

    private void runScripts(final Iterator<NumberedValue<A>> source, final Map<Long, R> results) {
        boolean isStarted = false;

        try {
            while (true) {
                NumberedValue<A> next;
                synchronized (results) {
                    if (!source.hasNext()) {
                        return;
                    }
                    next = source.next();
                }
                if (!isStarted) {
                    isStarted = true;
                    workerStarted();
                }

                final RunResult<R> result = run(next);

                synchronized (results) {
                    results.put((long) next.getNumber(), result.getResult());

                    if (result.getException() != null) {
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
    protected RunResult<R> run(final NumberedValue<A> arg) {
        try {
            return runner.run(arg.getNumber(), arg.getValue());
        } catch (final Throwable e) {
            final RunResult<R> res = new RunResult<>();
            res.setException(e);
            return res;
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
