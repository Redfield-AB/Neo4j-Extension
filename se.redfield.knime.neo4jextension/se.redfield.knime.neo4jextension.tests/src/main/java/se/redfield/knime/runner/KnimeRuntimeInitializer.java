/**
 *
 */
package se.redfield.knime.runner;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.internal.registry.RegistryProviderFactory;
import org.eclipse.core.internal.registry.osgi.RegistryProviderOSGI;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration.ConfigValues;
import org.eclipse.osgi.internal.location.BasicLocation;
import org.eclipse.osgi.service.datalocation.Location;
import org.knime.core.data.container.DefaultTableStoreFormat;
import org.osgi.util.tracker.ServiceTracker;

import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.ConnectorPortObjectSer;
import se.redfield.knime.neo4j.connector.ConnectorSpec;
import se.redfield.knime.neo4j.connector.ConnectorSpecSer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@SuppressWarnings("restriction")
class KnimeRuntimeInitializer implements Runnable {
    private static final ConfigValues configVars = new ConfigValues(new HashMap<>());

    /**
     * Default constructor.
     */
    public KnimeRuntimeInitializer() {
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
        //set initialized flag
        setFieldToPlatformInstance("initialized", Boolean.TRUE);
        //set bundle context
        setFieldToPlatformInstance("context", UnitTestBundle.INSTANCE);

        final String workDir = System.getProperty("user.dir");

        setLocationToPlatformInstance("configurationLocation", workDir);
        setLocationToPlatformInstance("installLocation", workDir);
        setLocationToPlatformInstance("instanceLocation", workDir);
        setLocationToPlatformInstance("userLocation", workDir);

        //init registries
        initRegistries();
    }

    private void initRegistries() throws CoreException {
        final JUnitExtensionRegistry reg = new JUnitExtensionRegistry();

        //add connector port extension
        final JUnitExtension connectorPort = new JUnitExtension();
        addConnectorPortObject(connectorPort);

        final JUnitExtension tableStoreFormat = new JUnitExtension();
        addDefaultFormat(tableStoreFormat);

        reg.getExtensionPoint(JUnitExtensionRegistry.PORT_TYPE).addExtension(
                "neo4j-connector-port", connectorPort);
        reg.getExtensionPoint(JUnitExtensionRegistry.TABLE_FORMAT).addExtension(
                "table-store-format", tableStoreFormat);

        RegistryProviderFactory.setDefault(new RegistryProviderOSGI(reg));
    }
    private void addDefaultFormat(final JUnitExtension ext) {
        final JunitConfigurationElement e = new JunitConfigurationElement();
        e.putAttribute("formatDefinition", DefaultTableStoreFormat.class.getName());
        e.putAttribute("objectClass", DefaultTableStoreFormat.class.getName());
        e.putAttribute("formatDefinition", DefaultTableStoreFormat.class.getName());

        ext.addConfigurationElement(e);
    }
    private void addConnectorPortObject(final JUnitExtension ext) {
        final JunitConfigurationElement e = new JunitConfigurationElement();
        e.putAttribute("color", "red");
        e.putAttribute("hidden", "false");
        e.putAttribute("name", "se.redfield.knime.neo4jextension.neo4jconnector");
        e.putAttribute("objectClass", ConnectorPortObject.class.getName());
        e.putAttribute("objectSerializer", ConnectorPortObjectSer.class.getName());
        e.putAttribute("specClass", ConnectorSpec.class.getName());
        e.putAttribute("specSerializer", ConnectorSpecSer.class.getName());

        ext.addConfigurationElement(e);
    }
    private void setLocationToPlatformInstance(final String fieldName,
            final String workDir) throws Exception {
        final ServiceTracker<Location, Location> tr = createLocationServiceTracker(fieldName, workDir);
        setFieldToPlatformInstance(fieldName, tr);
    }
    /**
     * @param location location.
     * @return service tracker for location service.
     */
    private ServiceTracker<Location,Location> createLocationServiceTracker(
            final String property, final String location)
            throws Exception {
        final ServiceTracker<Location, Location> st = new ServiceTracker<Location, Location>(
                UnitTestBundle.INSTANCE, Location.class.getName(), null);
        final BasicLocation loc = new BasicLocation(property, new File(location).toURI().toURL(),
                false, null, configVars, null,
                new AtomicBoolean(false));
        final Field f = ServiceTracker.class.getDeclaredField("cachedService");
        f.setAccessible(true);
        f.set(st, loc);
        return st;
    }
    private void setFieldToPlatformInstance(final String fieldName, final Object value)
            throws Exception {
        final Field platform = InternalPlatform.class.getDeclaredField("singleton");
        platform.setAccessible(true);

        final Field f = InternalPlatform.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(platform.get(null), value);
    }
}
