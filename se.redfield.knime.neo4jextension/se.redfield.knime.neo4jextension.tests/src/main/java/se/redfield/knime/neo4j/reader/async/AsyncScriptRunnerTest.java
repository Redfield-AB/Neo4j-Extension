/**
 *
 */
package se.redfield.knime.neo4j.reader.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import junit.framework.AssertionFailedError;
import se.redfield.knime.neo4j.db.AsyncScriptRunner;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncScriptRunnerTest extends AsyncScriptRunner<String> {
    private final Map<String, String> results = new HashMap<>();
    private final Map<String, RuntimeException> errors = new HashMap<>();

    public AsyncScriptRunnerTest() {
        super();
        setRunner(s -> runScriptImpl(s));
    }

    @Test
    public void testOutputOrder() {
        final int numScripts = 1000;

        final List<String> scripts = new LinkedList<>();
        for (int i = 0; i < numScripts; i++) {
            final String script = "script-" + i;

            scripts.add(script);
            results.put(script, script);
        }

        final Map<Long, String> result = run(scripts, 3);

        //check output has correct order
        for (int i = 0; i < numScripts; i++) {
            final String script = "script-" + i;
            assertEquals(script, result.get((long) i));
        }
    }
    @Test
    public void testStopOnQueryFailure() {
        setStopOnQueryFailure(true);

        final List<String> scripts = new LinkedList<>();

        //add correct responses
        final int firstGroup = 1000;
        for (int i = 0; i < firstGroup; i++) {
            final String script = "script-" + i;
            scripts.add(script);
            results.put(script, script);
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
            results.put(script, script);
        }

        //the number of threads should be same as num errors for be sure
        //all next result will not processed.
        final Map<Long, String> result = run(scripts, numErrors);

        assertTrue(hasErrors());
        assertTrue(result.size() < firstGroup + numErrors + 1);
    }
    @Test
    public void testNotStopOnQueryFailure() {
        setStopOnQueryFailure(false);

        final List<String> scripts = new LinkedList<>();

        //add correct responses
        final int firstGroup = 1000;
        for (int i = 0; i < firstGroup; i++) {
            final String script = "script-" + i;
            scripts.add(script);
            results.put(script, script);
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
            results.put(script, script);
        }

        //the number of threads should be same as num errors for be sure
        //all next result will not processed.
        final Map<Long, String> result = run(scripts, numErrors);

        assertTrue(hasErrors());

        //test first group
        for (int i = 0; i < firstGroup; i++) {
            final String script = "script-" + i;
            assertEquals(script, result.get((long) i));
        }

        //test errors
        for (int i = 0; i < numErrors; i++) {
            final long offset = firstGroup + i;
            assertNull(result.get(offset));
        }

        //test second groups.
        for (int i = 0; i < secondGroup; i++) {
            final long offset = firstGroup + numErrors + i;
            final String script = "script-" + offset;
            assertEquals(script, result.get(offset));
        }
    }
    @Test
    public void testExceptionOnZeroThreadPool() {
        final List<String> scripts = new LinkedList<String>();
        //empty list should be ok
        run(scripts, 0);

        scripts.add("script");
        try {
            run(scripts, 0);
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            //correct
        }
    }

    private String runScriptImpl(final String script) {
        if (errors.containsKey(script)) {
            throw errors.get(script);
        }
        return results.get(script);
    }
}
