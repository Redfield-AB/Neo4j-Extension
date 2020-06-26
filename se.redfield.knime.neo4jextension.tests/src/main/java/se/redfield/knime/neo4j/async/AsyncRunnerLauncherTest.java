/**
 *
 */
package se.redfield.knime.neo4j.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import junit.framework.AssertionFailedError;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncRunnerLauncherTest {
    private final Map<String, String> runScriptResults = new HashMap<>();
    private final Map<String, RuntimeException> errors = new HashMap<>();
    private final List<String> output = new LinkedList<>();
    private AsyncRunnerLauncher.Builder<String, String> builder;

    public AsyncRunnerLauncherTest() {
        super();
    }
    @Before
    public void setUp() {
        builder = AsyncRunnerLauncher.Builder.newBuilder(this::runScriptImpl)
                .withConsumer(this::addToOutput)
                .withNumThreads(20);
    }

    private void addToOutput(final String str) {
        if (str != null) {
            output.add(str);
        }
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

        builder
            .withNumThreads(3)
            .withSource(scripts.iterator())
            .withKeepSourceOrder(true)
            .build().run();

        //check output has correct order
        for (int i = 0; i < numScripts; i++) {
            final String script = "script-" + i;
            assertEquals(script, output.get(i));
        }
    }
    @Test
    public void statisticalTest() {
        //Create big lists of source items
        final List<String> source = new LinkedList<>();
        final int maxItems = 10000;
        for (int i = 0; i < maxItems; i++) {
            source.add("src-"+ i);
        }

        final Random random = new Random();
        final List<String> result = new LinkedList<>();

        //create 10 threads to process them
        builder
            .withRunner((value) -> {
                try {
                    //simulate the call to remote service.
                    Thread.sleep(1 + random.nextInt(30));
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return value;
            })
            .withConsumer(e -> {
                System.out.println("Next result added: " + e);
                result.add(e);
            })
            .withNumThreads(3)
            .withSource(source.iterator())
            .withKeepSourceOrder(true)
            .withStopOnFailure(true)
            .build().run();

        //test result
        final String[] array = result.toArray(new String[result.size()]);
        for (int i = 0; i < maxItems; i++) {
            assertEquals("src-"+ i, array[i]);
        }
    }
    @Test
    public void testStopOnQueryFailure() {
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
        final AsyncRunnerLauncher<String, String> launcher = builder
            .withStopOnFailure(true)
            .withNumThreads(numErrors)
            .withSource(scripts.iterator())
            .build();

        launcher.run();

        assertTrue(launcher.hasErrors());
        assertTrue(output.size() < firstGroup + numErrors + 1);
    }
    @Test
    public void testNotStopOnQueryFailure() {
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
        final AsyncRunnerLauncher<String, String> launcher = builder
            .withStopOnFailure(false)
            .withNumThreads(numErrors)
            .withSource(scripts.iterator())
            .build();

        launcher.run();

        assertTrue(launcher.hasErrors());

        final Set<String> output = new HashSet<>(this.output);
        assertEquals(firstGroup + secondGroup, output.size());

        //test first group
        for (int i = 0; i < firstGroup; i++) {
            final String script = "script-" + i;
            assertTrue(output.contains(script));
        }

        //test errors
        for (int i = 0; i < secondGroup; i++) {
            final String script = "script-" + (firstGroup + numErrors + i);
            assertTrue(output.contains(script));
        }

        //test second groups.
        for (int i = 0; i < secondGroup; i++) {
            final String script = "script-" + (firstGroup + numErrors + i);
            assertTrue(output.contains(script));
        }
    }
    @Test
    public void testExceptionOnZeroThreadPool() {
        final List<String> scripts = new LinkedList<String>();

        try {
            builder
                .withStopOnFailure(false)
                .withNumThreads(0)
                .withSource(scripts.iterator())
                .build();
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            //correct
        }
    }

    private String runScriptImpl(final String script) {
        if (errors.containsKey(script)) {
            throw errors.get(script);
        }
        return script;
    }
}
