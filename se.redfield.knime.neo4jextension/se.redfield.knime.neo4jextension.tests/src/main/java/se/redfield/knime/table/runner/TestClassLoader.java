/**
 *
 */
package se.redfield.knime.table.runner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class TestClassLoader extends URLClassLoader implements BundleReference {
    public TestClassLoader() {
        super(calculateClassPath(), TestClassLoader.class.getClassLoader());
        fixClassLoader(this);
        fixClassLoader(TestClassLoader.class.getClassLoader());
    }
    private static URL[] calculateClassPath() {
        return ((URLClassLoader) TestClassLoader.class.getClassLoader()).getURLs();
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (shouldProcess(name)) {
            synchronized (getClassLoadingLock(name)) {
                // First, check if the class has already been loaded
                try {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) {
                        c = findClass(name);
                        resolveClass(c);
                    }
                    return c;
                } catch (final ClassNotFoundException e) {
                }
            }
        }
        return super.loadClass(name);
    }
    /**
     * @param name class name.
     * @return true if should disable certificates.
     */
    private boolean shouldProcess(final String name) {
        final Set<String> prefixes = new HashSet<String>();
        prefixes.add("org.osgi.framework");
        prefixes.add("org.junit.");
        prefixes.add("junit.");
        prefixes.add("org.apache.log4j");
        for (final String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return false;
            }
        }

        return true;
    }
    public static void launchMe(final Class<?> cl, final String methodName) {
        @SuppressWarnings("resource")
        final
        TestClassLoader loader = new TestClassLoader();
        try {
            final Class<?> clazz = loader.loadClass(cl.getName());
            clazz.getMethod(methodName).invoke(null);
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public Bundle getBundle() {
        return UnitTestBundle.INSTANCE;
    }

    /**
     * @param loader class loader to disable certificate verification
     */
    private static void fixClassLoader(final ClassLoader loader) {
        try {
            final Field p2c = ClassLoader.class.getDeclaredField("package2certs");
            setFieldNotFinal(p2c);

            p2c.setAccessible(true);
            p2c.set(loader, new AlwaysEmptyMap<String, Certificate[]>());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static void setFieldNotFinal(final Field f) throws Exception {
        final Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);

        final int access = modifiers.getInt(f);
        modifiers.setInt(f, access & ~Modifier.FINAL );
    }
}
