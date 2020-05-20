/**
 *
 */
package se.redfield.knime.neo4j.reader.async;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;

import se.redfield.knime.neo4j.db.DataAdapter;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.reader.ReaderModel;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncScriptRunner {
    private final Driver driver;
    private boolean stopOnQueryFailure;
    private boolean hasErrors;

    public AsyncScriptRunner(final Driver driver) {
        super();
        this.driver = driver;
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
    public Map<Long, String> run(final List<String> scripts, final int originMumThreads) {
        if (scripts.isEmpty()) {
            return new HashMap<>();
        }

        final int numThreads = Math.min(scripts.size(), originMumThreads);
        if (numThreads < 1) {
            throw new IllegalArgumentException("Number of threads " + numThreads + " < 1");
        }

        final int[] numThreadsHolder = {numThreads};
        final Iterator<NumberedString> iter = createNumberedIterator(scripts);
        final Map<Long, String> result = new HashMap<>();

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

    private void runScripts(final Iterator<NumberedString> source, final Map<Long, String> result) {
        while (true) {
            NumberedString next;
            synchronized (result) {
                if (!source.hasNext()) {
                    return;
                }
                next = source.next();
            }

            final long offset = next.getNumber();
            try {
                final String json = runScript(next.getString());
                synchronized (result) {
                    result.put(offset, json);
                }
            } catch (final Throwable e) {
                e.printStackTrace();
                synchronized (result) {
                    result.put(offset, null);
                    hasErrors = true;
                    if (isStopOnQueryFailure()) {
                        readToEnd(source);
                    }
                }
            }
        }
    }

    private String runScript(final String script) {
        final List<Record> records = Neo4jSupport.runRead(driver, script, null);
        final String json = ReaderModel.buildJson(records,
                new DataAdapter(driver.defaultTypeSystem()));
        return json;
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
