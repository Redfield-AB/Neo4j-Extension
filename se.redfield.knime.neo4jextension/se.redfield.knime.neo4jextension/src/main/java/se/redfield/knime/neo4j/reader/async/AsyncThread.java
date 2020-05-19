/**
 *
 */
package se.redfield.knime.neo4j.reader.async;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncThread extends Thread {
    private final AsyncLauncher source;

    public AsyncThread(final AsyncLauncher source) {
        super("Async worker");
        this.source = source;
    }

    @Override
    public void run() {
        try {
            while (true) {
                source.execute();
            }
        } catch (final StopException e) {
        }
    }
}
