/**
 *
 */
package se.redfield.knime.neo4j.db;

import org.knime.core.node.ExecutionContext;
import org.neo4j.driver.Driver;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ContextListeningDriver {
    private final Driver driver;
    private final ExecutionContext context;
    private boolean isClosed = false;

    ContextListeningDriver(final Driver driver, final ExecutionContext context) {
        super();
        this.driver = driver;
        this.context = context;

        //start context listener thread
        final Thread t = new Thread(() -> runContextListenerThread(), "");
        t.start();
    }

    private synchronized void runContextListenerThread() {
        while (true) {
            if (isClosed) {
                return;
            }
            try {
                context.checkCanceled();
            } catch (final Throwable e) {
                urgentClose();
                return;
            }

            try {
                wait(500);
            } catch (final InterruptedException e) {
                return;
            }
        }
    }
    private void urgentClose() {
        isClosed = true;
        try {
            driver.close();
        } catch (final Throwable e) {
        }
    }
    public void setProgress(final double progress) {
        context.setProgress(progress);
    }
    public synchronized void close() {
        if (!isClosed) {
            isClosed = true;
            notifyAll();
            try {
                driver.closeAsync();
            } catch (final Throwable e) {
            }
            context.setProgress(1.0);
        }
    }
    /**
     * @return Neo4j driver
     */
    public Driver getDriver() {
        return driver;
    }
}
