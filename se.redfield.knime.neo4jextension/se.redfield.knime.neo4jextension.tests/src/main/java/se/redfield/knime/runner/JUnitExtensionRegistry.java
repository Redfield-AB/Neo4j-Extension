/**
 *
 */
package se.redfield.knime.runner;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.IRegistryEventListener;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class JUnitExtensionRegistry implements IExtensionRegistry {
    public static final String TABLE_FORMAT = "org.knime.core.TableFormat";
    public static final String COLUMN_META_DATA = "org.knime.core.DataColumnMetaDataType";
    public static final String PORT_TYPE = "org.knime.core.PortType";

    private final Map<String, JUnitExtensionPoint> extensionPoints = new HashMap<>();

    /**
     * Default constructor.
     */
    public JUnitExtensionRegistry() {
        super();
        //add extension points
        extensionPoints.put(PORT_TYPE, new JUnitExtensionPoint());
        extensionPoints.put(COLUMN_META_DATA, new JUnitExtensionPoint());
        extensionPoints.put(TABLE_FORMAT, new JUnitExtensionPoint());
    }

    @Override
    public void addRegistryChangeListener(final IRegistryChangeListener listener, final String namespace) {
    }
    @Override
    public void addRegistryChangeListener(final IRegistryChangeListener listener) {
    }
    @Override
    public IConfigurationElement[] getConfigurationElementsFor(final String extensionPointId) {
        return null;
    }
    @Override
    public IConfigurationElement[] getConfigurationElementsFor(final String namespace, final String extensionPointName) {
        return null;
    }
    @Override
    public IConfigurationElement[] getConfigurationElementsFor(final String namespace, final String extensionPointName,
            final String extensionId) {
        return null;
    }
    @Override
    public IExtension getExtension(final String extensionId) {
        for (final JUnitExtensionPoint p : getExtensionPoints()) {
            final IExtension ext = p.getExtension(extensionId);
            if (ext != null) {
                return ext;
            }
        }
        return null;
    }
    @Override
    public IExtension getExtension(final String extensionPointId, final String extensionId) {
        return getExtension(extensionId);
    }
    @Override
    public IExtension getExtension(final String namespace, final String extensionPointName, final String extensionId) {
        return getExtension(extensionId);
    }
    @Override
    public JUnitExtensionPoint getExtensionPoint(final String extensionPointId) {
        return extensionPoints.get(extensionPointId);
    }
    @Override
    public IExtensionPoint getExtensionPoint(final String namespace, final String extensionPointName) {
        return null;
    }
    @Override
    public JUnitExtensionPoint[] getExtensionPoints() {
        final List<JUnitExtensionPoint> points = new LinkedList<>(extensionPoints.values());
        return points.toArray(new JUnitExtensionPoint[points.size()]);
    }
    @Override
    public IExtensionPoint[] getExtensionPoints(final String namespace) {
        return getExtensionPoints();
    }
    @Override
    public IExtensionPoint[] getExtensionPoints(final IContributor contributor) {
        return getExtensionPoints();
    }
    @Override
    public IExtension[] getExtensions(final String namespace) {
        final List<IExtension> points = new LinkedList<>();
        for (final JUnitExtensionPoint p : getExtensionPoints()) {
            points.addAll(Arrays.asList(p.getExtensions()));
        }
        return points.toArray(new IExtension[points.size()]);
    }
    @Override
    public IExtension[] getExtensions(final IContributor contributor) {
        return getExtensions("");
    }
    @Override
    public String[] getNamespaces() {
        return new String[0];
    }
    @Override
    public void removeRegistryChangeListener(final IRegistryChangeListener listener) {
    }
    @Override
    public boolean addContribution(final InputStream is, final IContributor contributor, final boolean persist, final String name,
            final ResourceBundle translationBundle, final Object token) throws IllegalArgumentException {
        return false;
    }
    @Override
    public boolean removeExtension(final IExtension extension, final Object token) throws IllegalArgumentException {
        return false;
    }
    @Override
    public boolean removeExtensionPoint(final IExtensionPoint extensionPoint, final Object token) throws IllegalArgumentException {
        return false;
    }
    @Override
    public void stop(final Object token) throws IllegalArgumentException {
    }
    @Override
    public void addListener(final IRegistryEventListener listener) {
    }
    @Override
    public void addListener(final IRegistryEventListener listener, final String extensionPointId) {
    }
    @Override
    public void removeListener(final IRegistryEventListener listener) {
    }
    @Override
    public boolean isMultiLanguage() {
        return false;
    }
}
