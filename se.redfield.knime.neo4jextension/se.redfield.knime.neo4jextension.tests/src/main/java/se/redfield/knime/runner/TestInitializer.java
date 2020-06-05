/**
 *
 */
package se.redfield.knime.runner;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration.ConfigValues;
import org.eclipse.osgi.internal.location.BasicLocation;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@SuppressWarnings("restriction")
class TestInitializer implements Runnable {
    private static final ConfigValues configVars = new ConfigValues(new HashMap<>());

    /**
     * Default constructor.
     */
    public TestInitializer() {
        super();
    }
    @Override
    public void run() {
        initLog4j();
        try {
            hackKnime();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void initLog4j() {
        System.setProperty("log4j.configuration", "log4j.xml");
    }
    private void hackKnime() throws Exception {
        setPlatformInitialized();

        //set bundle context
        setFieldToPlatformInstance("context", UnitTestBundle.INSTANCE);

        final String workDir = System.getProperty("user.dir");

        setLocationToPlatformInstance("configurationLocation", workDir);
        setLocationToPlatformInstance("installLocation", workDir);
        setLocationToPlatformInstance("instanceLocation", workDir);
        setLocationToPlatformInstance("userLocation", workDir);
    }

    private void setLocationToPlatformInstance(final String fieldName,
            final String workDir) throws Exception {
        final ServiceTracker<Location, Location> tr = createLocationServiceTracker(fieldName, workDir);
        setFieldToPlatformInstance(fieldName, tr);
    }
    /**
     * @param workDir
     * @return
     */
    private ServiceTracker<Location,Location> createLocationServiceTracker(
            final String property, final String workDir)
            throws Exception {
        final ServiceTracker<Location, Location> st = new ServiceTracker<Location, Location>(
                UnitTestBundle.INSTANCE, Location.class.getName(), null);
        final BasicLocation loc = new BasicLocation(property, new File(workDir).toURI().toURL(),
                false, null, configVars, null,
                new AtomicBoolean(false));
        final Field f = ServiceTracker.class.getDeclaredField("cachedService");
        f.setAccessible(true);
        f.set(st, loc);
        return st;
    }
    private void setFieldToPlatformInstance(final String fieldName, final Object value)
            throws Exception {
        final Class<?> cls = getEclipsePlatformImplClass();

        final Field platform = cls.getDeclaredField("singleton");
        platform.setAccessible(true);

        final Field f = cls.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(platform.get(null), value);
    }
    /**
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void setPlatformInitialized() throws Exception {
        final Class<?> cls = getEclipsePlatformImplClass();
        //set initialized
        final Field f = cls.getDeclaredField("initialized");
        f.setAccessible(true);
        f.set(null, Boolean.TRUE);
    }
    /**
     * @return
     * @throws Exception
     */
    private Class<?> getEclipsePlatformImplClass() throws Exception {
        return TestInitializer.class.getClassLoader().loadClass(
                "org.eclipse.core.internal.runtime.InternalPlatform");
    }
}
