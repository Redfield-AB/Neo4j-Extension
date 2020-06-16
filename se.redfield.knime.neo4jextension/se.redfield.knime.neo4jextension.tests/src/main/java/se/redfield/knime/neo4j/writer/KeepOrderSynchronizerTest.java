/**
 *
 */
package se.redfield.knime.neo4j.writer;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import se.redfield.knime.neo4j.async.AsyncRunnerLauncher;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class KeepOrderSynchronizerTest {
    /**
     * Default constructor.
     */
    public KeepOrderSynchronizerTest() {
        super();
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
        final KeepOrderSynchronizer<String, String> sync = new KeepOrderSynchronizer<>(
                e -> {
                    System.out.println("Next result added: " + e);
                    result.add(e);
                }, source.iterator());
        final AsyncRunnerLauncher<String> launcher = AsyncRunnerLauncher.createLauncher(
                (num, value) -> {
                    try {
                        //simulate the call to remote service.
                        Thread.sleep(1 + random.nextInt(30));
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    sync.addToOutput(num, value);
                }, sync.getSynchronizedIterator(), 10) ;
        launcher.setStopOnFailure(true);
        launcher.setSyncObject(sync.getSynchronizedIterator());

        launcher.run();

        //test result
        final String[] array = result.toArray(new String[result.size()]);
        for (int i = 0; i < maxItems; i++) {
            assertEquals("src-"+ i, array[i]);
        }
    }
}
