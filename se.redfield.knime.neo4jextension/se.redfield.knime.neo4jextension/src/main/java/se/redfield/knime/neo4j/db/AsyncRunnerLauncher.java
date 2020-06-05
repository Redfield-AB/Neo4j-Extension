/**
 *
 */
package se.redfield.knime.neo4j.db;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncRunnerLauncher<R, A> {
    private boolean stopOnQueryFailure;
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

    public void setStopOnQueryFailure(final boolean stop) {
        this.stopOnQueryFailure = stop;
    }
    public boolean isStopOnQueryFailure() {
        return stopOnQueryFailure;
    }

    /**
     * @param arguments list of arguments to execute.
     * @param originMumThreads number of threads.
     * @return map of execution results to its position in origin list.
     */
    public Map<Long, R> run(final List<A> arguments, final int originMumThreads) {
        if (arguments.isEmpty()) {
            return new HashMap<>();
        }

        final int numThreads = Math.min(arguments.size(), originMumThreads);
        if (numThreads < 1) {
            throw new IllegalArgumentException("Number of threads " + numThreads + " < 1");
        }

        final int[] numThreadsHolder = {numThreads};
        final Iterator<NumberedArgument<A>> iter = createNumberedIterator(arguments);
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

    private Iterator<NumberedArgument<A>> createNumberedIterator(final List<A> args) {
        int pos = 0;
        final List<NumberedArgument<A>> result = new LinkedList<>();
        for (final A arg : args) {
            result.add(new NumberedArgument<A>(pos, arg));
            pos++;
        }
        return result.iterator();
    }

    private void runScripts(final Iterator<NumberedArgument<A>> source, final Map<Long, R> results) {
        while (true) {
            NumberedArgument<A> next;
            synchronized (results) {
                if (!source.hasNext()) {
                    return;
                }
                next = source.next();
            }

            final RunResult<R> result = runScript(next.getArgument());

            synchronized (results) {
                results.put((long) next.getNumber(), result.getResult());

                if (result.getException() != null) {
                    hasErrors = true;
                    if (isStopOnQueryFailure()) {
                        readToEnd(source);
                    }
                }
            }
        }
    }

    protected RunResult<R> runScript(final A script) {
        try {
            return runner.run(script);
        } catch (final Throwable e) {
            final RunResult<R> res = new RunResult<>();
            res.setException(e);
            return res;
        }
    }

    private void readToEnd(final Iterator<NumberedArgument<A>> iter) {
        while (iter.hasNext()) {
            iter.next();
        }
    }
    private Thread createThread(final int[] numThreads, final Runnable runnable) {
        return new Thread("Async script runner") {
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
