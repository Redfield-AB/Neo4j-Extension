/**
 *
 */
package se.redfield.knime.neo4j.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import junit.framework.AssertionFailedError;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncRunnerLauncherTest extends AsyncRunnerLauncher<String> {
    private final Map<String, String> runScriptResults = new HashMap<>();
    private final Map<String, RuntimeException> errors = new HashMap<>();
    private final Map<Long, String> output = new ConcurrentHashMap<>();
    private static final IteratorProxy<String> ITERATOR = new IteratorProxy<>();

    public AsyncRunnerLauncherTest() {
        super(ITERATOR, 20);
        setRunner(this::runScriptImpl);
    }

    @Test
    public void testOutputOrder() {
        final int numScripts = 1000;

        final List<String> scripts = new LinkedList<>();
        for (int i = 0; i < numScripts; i++) {
            final String script = "script-" + i;

            scripts.add(script);
            runScriptResults.put(script, script);
        }

        setNumThreads(3);
        ITERATOR.setIterator(scripts.iterator());
        run();

        //check output has correct order
        for (int i = 0; i < numScripts; i++) {
            final String script = "script-" + i;
            assertEquals(script, output.get((long) i));
        }
    }
    @Test
    public void testStopOnQueryFailure() {
        setStopOnFailure(true);

        final List<String> scripts = new LinkedList<>();

        //add correct responses
        final int firstGroup = 1000;
        for (int i = 0; i < firstGroup; i++) {
            final String script = "script-" + i;
            scripts.add(script);
            runScriptResults.put(script, script);
        }

        final int numErrors = 3;
        for (int i = 0; i < numErrors; i++) {
            final String script = "script-" + (firstGroup + i);
            scripts.add(script);
            errors.put(script, new RuntimeException("any error"));
        }

        //add second group of correct responses.
        for (int i = 0; i < 1000; i++) {
            final String script = "script-" + (firstGroup + numErrors + i);
            scripts.add(script);
            runScriptResults.put(script, script);
        }

        //the number of threads should be same as num errors for be sure
        //all next result will not processed.
        setNumThreads(numErrors);
        ITERATOR.setIterator(scripts.iterator());
        run();

        assertTrue(hasErrors());
        assertTrue(output.size() < firstGroup + numErrors + 1);
    }
    @Test
    public void testNotStopOnQueryFailure() {
        setStopOnFailure(false);

        final List<String> scripts = new LinkedList<>();

        //add correct responses
        final int firstGroup = 1000;
        for (int i = 0; i < firstGroup; i++) {
            final String script = "script-" + i;
            scripts.add(script);
            runScriptResults.put(script, script);
        }

        final int numErrors = 3;
        for (int i = 0; i < numErrors; i++) {
            final String script = "script-" + (firstGroup + i);
            scripts.add(script);
            errors.put(script, new RuntimeException("any error"));
        }

        //add second group of correct responses.
        final int secondGroup = 1000;
        for (int i = 0; i < secondGroup; i++) {
            final String script = "script-" + (firstGroup + numErrors + i);
            scripts.add(script);
            runScriptResults.put(script, script);
        }

        //the number of threads should be same as num errors for be sure
        //all next result will not processed.
        setNumThreads(numErrors);
        ITERATOR.setIterator(scripts.iterator());
        run();

        assertTrue(hasErrors());

        //test first group
        for (int i = 0; i < firstGroup; i++) {
            final String script = "script-" + i;
            assertEquals(script, output.get((long) i));
        }

        //test errors
        for (int i = 0; i < numErrors; i++) {
            final long offset = firstGroup + i;
            assertNull(output.get(offset));
        }

        //test second groups.
        for (int i = 0; i < secondGroup; i++) {
            final long offset = firstGroup + numErrors + i;
            final String script = "script-" + offset;
            assertEquals(script, output.get(offset));
        }
    }
    @Test
    public void testExceptionOnZeroThreadPool() {
        final List<String> scripts = new LinkedList<String>();
        //empty list should be ok
        setNumThreads(0);
        ITERATOR.setIterator(scripts.iterator());

        run();

        scripts.add("script");
        ITERATOR.setIterator(scripts.iterator());
        try {
            run();
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            //correct
        }
    }

    private void runScriptImpl(final long num, final String script) {
        if (errors.containsKey(script)) {
            throw errors.get(script);
        }
        output.put(num, script);
    }
}
