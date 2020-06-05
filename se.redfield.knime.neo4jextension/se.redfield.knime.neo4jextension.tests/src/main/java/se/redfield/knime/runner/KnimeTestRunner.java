/**
 *
 */
package se.redfield.knime.runner;

import java.lang.reflect.Constructor;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Warning!!!
 * This is development scope test runner. Please not use it as part of serious
 * tests for covering the project.
 * This runner minimally populates an Eclipse and Knime classes for be possible
 * run simple classes without run heavy Eclipse Plugin Tests.
 * The runner uses reflection mechanism and may stop be actual. Please remove it
 * in this case and also remove the tests which use it.
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
