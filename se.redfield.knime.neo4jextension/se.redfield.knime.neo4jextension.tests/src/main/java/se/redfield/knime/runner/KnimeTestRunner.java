/**
 *
 */
package se.redfield.knime.runner;

import java.lang.reflect.Constructor;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Warning!!!
 * This test runner is in fact hacking. I know it, but it is better to use fast
 * unit test then use standard Eclipse Test runner. It saves your time.
 * If anything did stop to work, please debug it and correct KNime initialization.
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
        super(loadTestClass(klass.getName()));
        final Runnable  init = loadRunnable(KnimeRuntimeInitializer.class.getName());
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
