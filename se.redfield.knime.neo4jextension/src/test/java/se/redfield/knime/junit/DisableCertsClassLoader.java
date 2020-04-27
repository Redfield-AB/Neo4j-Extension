/**
 *
 */
package se.redfield.knime.junit;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Manifest;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class DisableCertsClassLoader extends URLClassLoader {
    private boolean initialized;

    /**
     * @param urls
     * @param parent
     */
    public DisableCertsClassLoader() {
        super(calculateClassPath(),
                DisableCertsClassLoader.class.getClassLoader().getParent());
    }
    private static URL[] calculateClassPath() {
        final URL[] originUrls = ((URLClassLoader) DisableCertsClassLoader.class.getClassLoader()).getURLs();
        final URL[] urls = new URL[originUrls.length + 1];
        System.arraycopy(originUrls, 0, urls, 0, originUrls.length);
        urls[urls.length - 1] = DisableCertsClassLoader.class.getClassLoader().getResource("hacks.jar");
        return urls;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (!shouldDisableCerts(name)) {
            return super.findClass(name);
        }

        final String path = name.replace('.', '/').concat(".class");
        final Object res = getResourceImpl(path);
        if (res != null) {
            try {
                return defineClassImpl(name, res);
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
     * @throws ClassNotFoundException
     * @throws InstantiationException
     */
    private Class<?> defineClassImpl(final String name, final Object originRes)
            throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, ClassNotFoundException, InstantiationException {
        final Object res = wrapByResourceExt(originRes);
        final Class<?> resourceClass = getParent().loadClass("sun.misc.Resource");
        final Method m = URLClassLoader.class.getDeclaredMethod(
                "defineClass", String.class, resourceClass);
        m.setAccessible(true);
        return (Class<?>) m.invoke(this, name, res);
    }
    private Object wrapByResourceExt(final Object origin)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final Class<?> clazz = loadClass("se.redfield.knime.neo4jextension.ResourceExt");
        final Constructor<?> cons = clazz.getConstructors()[0];
        return cons.newInstance(origin);
    }
    /**
     * @param path
     * @throws ClassNotFoundException
     */
    private Object getResourceImpl(final String path) throws ClassNotFoundException {
        try {
            final Object ucp = getMyProperty(this, this.getClass(), "ucp");

            final Class<?> clazz = loadClass("sun.misc.URLClassPath");
            final Method m = clazz.getDeclaredMethod("getResource", String.class, boolean.class);
            m.setAccessible(true);
            return m.invoke(ucp, path, Boolean.FALSE);
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
    private boolean shouldDisableCerts(final String name) {
        return name.startsWith("org.knime.");
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

    @Override
    protected Package definePackage(final String name, final Manifest man, final URL url)
            throws IllegalArgumentException {
        ensureInitialized();
        return super.definePackage(name, man, url);
    }
    @Override
    protected Package definePackage(final String name, final String specTitle, final String specVersion,
            final String specVendor, final String implTitle, final String implVersion,
            final String implVendor, final URL sealBase) throws IllegalArgumentException {
        ensureInitialized();
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }
    @Override
    public URL findResource(final String name) {
        ensureInitialized();
        return super.findResource(name);
    }
    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        ensureInitialized();
        return super.findResources(name);
    }
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        ensureInitialized();
        return super.loadClass(name, resolve);
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized =true;

        try {
            final Object context = loadClass(
                    "se.redfield.knime.junit.MockBundleContext").newInstance();
            final Method setProp = context.getClass().getDeclaredMethod("setProperty",
                    String.class, String.class);
            setProp.invoke(context, "osgi.os", "Linux");

            final Class<?> clazz = loadClass("org.eclipse.core.internal.runtime.InternalPlatform");
            Field field = clazz.getDeclaredField("singleton");
            field.setAccessible(true);

            final Object platform = field.get(null);
            field = clazz.getDeclaredField("context");
            field.setAccessible(true);
            field.set(platform, context);
        } catch (final Exception e) {
            initialized = false;
            throw new RuntimeException(e);
        }
    }
}
