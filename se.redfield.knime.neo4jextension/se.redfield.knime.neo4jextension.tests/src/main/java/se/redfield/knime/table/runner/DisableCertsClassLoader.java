/**
 *
 */
package se.redfield.knime.table.runner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.runners.BlockJUnit4ClassRunner;

import sun.misc.Resource;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class DisableCertsClassLoader extends URLClassLoader {
    /**
     * @param urls
     * @param parent
     */
    public DisableCertsClassLoader() {
        super(calculateClassPath(),
                DisableCertsClassLoader.class.getClassLoader().getParent());
    }
    private static URL[] calculateClassPath() {
        return ((URLClassLoader) DisableCertsClassLoader.class.getClassLoader()).getURLs();
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (!shouldProcess(name)) {
            return BlockJUnit4ClassRunner.class.getClassLoader().loadClass(name);
        }
        return super.loadClass(name);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        final String path = name.replace('.', '/').concat(".class");
        final Resource res = getResourceImpl(path);
        if (res != null) {
            try {
                final Class<?> found = defineClassImpl(name, new ResourceExt(res));
                System.out.println(found.getName() + " loaded");
                return found;
            } catch (final Throwable e) {
                throw new ClassNotFoundException(name, e);
            }
        } else {
            return null;
        }
    }
    /**
     * @param name
     * @param res
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private Class<?> defineClassImpl(final String name, final Resource res)
            throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//        private Class<?> defineClass(String name, Resource res)
        final Method m = URLClassLoader.class.getDeclaredMethod(
                "defineClass", String.class, Resource.class);
        m.setAccessible(true);
        return (Class<?>) m.invoke(this, name, res);
    }
    /**
     * @param path
     * @throws ClassNotFoundException
     */
    private Resource getResourceImpl(final String path) throws ClassNotFoundException {
        try {
            final Object ucp = getMyProperty(this, this.getClass(), "ucp");

            final Class<?> clazz = loadClass("sun.misc.URLClassPath");
            final Method m = clazz.getDeclaredMethod("getResource", String.class, boolean.class);
            m.setAccessible(true);
            return (Resource) m.invoke(ucp, path, Boolean.FALSE);
        } catch (final Exception e) {
            throw new ClassNotFoundException(path);
        }
    }
    private Object getMyProperty(final Object obj,
            final Class<?> clazz, final String fieldName) throws IllegalArgumentException, IllegalAccessException {
        if (clazz == Object.class || clazz == null) {
            throw new RuntimeException("Field not found: " + fieldName);
        }

        try {
            final Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (final NoSuchFieldException e) {
            return getMyProperty(obj, clazz.getSuperclass(), fieldName);
        }
    }
    /**
     * @param name class name.
     * @return true if should disable certificates.
     */
    private boolean shouldProcess(final String name) {
        final boolean should = !(name.startsWith("org.junit.") || name.startsWith("junit."));
        return should;
    }
    public static void launchMe(final Class<?> cl, final String methodName) {
        @SuppressWarnings("resource")
        final
        DisableCertsClassLoader loader = new DisableCertsClassLoader();
        try {
            final Class<?> clazz = loader.loadClass(cl.getName());
            clazz.getMethod(methodName).invoke(null);
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
