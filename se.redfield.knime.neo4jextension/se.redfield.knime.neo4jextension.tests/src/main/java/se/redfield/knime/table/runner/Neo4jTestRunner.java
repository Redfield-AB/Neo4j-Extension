/**
 *
 */
package se.redfield.knime.table.runner;

import java.lang.reflect.Constructor;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4jTestRunner extends BlockJUnit4ClassRunner {
    private static final TestClassLoader loader = new TestClassLoader();

    /**
     * @param klass testing class.
     */
    public Neo4jTestRunner(final Class<?> klass) throws InitializationError {
        super(loadTestClass(klass.getName()));
        final Runnable  init = loadRunnable(TestInitializer.class.getName());
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
