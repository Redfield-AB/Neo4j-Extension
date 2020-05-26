/**
 *
 */
package se.redfield.knime.table.runner;

import java.lang.reflect.Field;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class TestInitializer implements Runnable {
    private static final String DBUSER = "neo4j";
    private static final String PASSWORD = "password";
    private static final String URL = "bolt://localhost:7687";

    /**
     * Default constructor.
     */
    public TestInitializer() {
        super();
    }
    @Override
    public void run() {
        initLog4j();
        initNeo4j();
        try {
            initKnime();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
    private void initLog4j() {
        System.setProperty("log4j.configuration", "log4j.xml");
    }
    private void initKnime() throws Exception {
        final Class<?> cls = loadClass("org.eclipse.core.internal.runtime.InternalPlatform");

        //set initialized
        Field f = cls.getDeclaredField("initialized");
        f.setAccessible(true);
        f.set(null, Boolean.TRUE);

        //set bundle context
        final Field platform = cls.getDeclaredField("singleton");
        platform.setAccessible(true);

        f = cls.getDeclaredField("context");
        f.setAccessible(true);
        final TestBundleContext context = new TestBundleContext();
        f.set(platform.get(null), context);

        final String workDir = System.getProperty("user.dir");
        //TODO add mock service trackers.
//
//        setTestLocation(cls, f, context, "",  workDir);
//        private final ServiceTracker<Location,Location> configurationLocation = null;
//        private final ServiceTracker<Location,Location> installLocation = null;
//        private final ServiceTracker<Location,Location> instanceLocation = null;
//        private final ServiceTracker<Location,Location> userLocation = null;

    }
    private Class<?> loadClass(final String name) throws Exception{
        return TestInitializer.class.getClassLoader().loadClass(name);
    }
    private void initNeo4j() {
        final Neo4JTestContext ctxt = new Neo4JTestContext(URL, DBUSER, PASSWORD);
        Neo4JTestContext.setCurrent(ctxt);
    }
}
