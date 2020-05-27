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
public class AsyncScriptRunner<R> {
    private boolean stopOnQueryFailure;
    private boolean hasErrors;
    private SingleScriptRunner<R> runner;

    public AsyncScriptRunner(final SingleScriptRunner<R> runner) {
        super();
        this.runner = runner;
    }
    //just for unit tests
    protected AsyncScriptRunner() {
        super();
    }
    protected void setRunner(final SingleScriptRunner<R> runner) {
        this.runner = runner;
    }

    public void setStopOnQueryFailure(final boolean stop) {
        this.stopOnQueryFailure = stop;
    }
    public boolean isStopOnQueryFailure() {
        return stopOnQueryFailure;
    }

    /**
     * @param scripts list of scripts to execute.
     * @param originMumThreads number of threads.
     * @return map of execution results to its position in origin list.
     */
    public Map<Long, R> run(final List<String> scripts, final int originMumThreads) {
        if (scripts.isEmpty()) {
            return new HashMap<>();
        }

        final int numThreads = Math.min(scripts.size(), originMumThreads);
        if (numThreads < 1) {
            throw new IllegalArgumentException("Number of threads " + numThreads + " < 1");
        }

        final int[] numThreadsHolder = {numThreads};
        final Iterator<NumberedString> iter = createNumberedIterator(scripts);
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

    private Iterator<NumberedString> createNumberedIterator(final List<String> scripts) {
        int pos = 0;
        final List<NumberedString> result = new LinkedList<>();
        for (final String script : scripts) {
            result.add(new NumberedString(pos, script));
            pos++;
        }
        return result.iterator();
    }

    private void runScripts(final Iterator<NumberedString> source, final Map<Long, R> results) {
        while (true) {
            NumberedString next;
            synchronized (results) {
                if (!source.hasNext()) {
                    return;
                }
                next = source.next();
            }

            final long offset = next.getNumber();
            try {
                final R result = runScript(next.getString());
                synchronized (results) {
                    results.put(offset, result);
                }
            } catch (final Throwable e) {
                synchronized (results) {
                    results.put(offset, null);
                    hasErrors = true;
                    if (isStopOnQueryFailure()) {
                        readToEnd(source);
                    }
                }
            }
        }
    }

    protected R runScript(final String script) {
        return runner.run(script);
    }

    private void readToEnd(final Iterator<NumberedString> iter) {
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
