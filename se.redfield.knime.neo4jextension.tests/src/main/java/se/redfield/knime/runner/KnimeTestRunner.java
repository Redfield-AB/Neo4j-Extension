/**
 *
 */
package se.redfield.knime.runner;

import java.lang.reflect.Constructor;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import se.redfield.knime.runner.knime.KnimeInitializer;

/**
 * Warning!!!
 * This test runner is hacking in fact? I know it! But it is better to use fast
 * unit tests instead of standard Eclipse Test runner. It saves your time.
 * If anything did stop to work, please debug it and correct KNime test initializer.
 *
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class KnimeTestRunner extends BlockJUnit4ClassRunner {
    private static final TestClassLoader loader = new TestClassLoader();

    /**
     * @param klass testing class.
     */
    public KnimeTestRunner(final Class<?> klass) throws InitializationError {
        //load test class again by special class loader
        super(loadTestClass(klass.getName()));
        //load initializer by special class loader.
        final Runnable  init = loadRunnable(KnimeInitializer.class.getName());
        init.run();
    }
    private Runnable loadRunnable(final String name) {
        try {
            final Class<?> clazz = loadTestClass(name);
            final Constructor<?> con = clazz.getConstructor();
            con.setAccessible(true);
            return (Runnable) con.newInstance();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static Class<?> loadTestClass(final String name) throws InitializationError {
        try {
            return loader.loadClass(name);
        } catch (final ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }
}
