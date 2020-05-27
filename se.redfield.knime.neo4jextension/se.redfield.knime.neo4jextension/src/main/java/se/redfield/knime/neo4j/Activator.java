/**
 *
 */
package se.redfield.knime.neo4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import se.redfield.knime.neo4j.utils.ExecutorServiceImpl;
import se.redfield.knime.neo4j.utils.ThreadPool;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Activator implements BundleActivator {
    @Override
    public void start(final BundleContext context) throws Exception {
        ((ExecutorServiceImpl) ThreadPool.getExecutor()).start();
    }
    @Override
    public void stop(final BundleContext context) throws Exception {
        final ExecutorService exe = ThreadPool.getExecutor();
        exe.shutdown();
        exe.awaitTermination(5, TimeUnit.MINUTES);
    }
}
