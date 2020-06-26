/**
 *
 */
package se.redfield.knime.runner.knime;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class JUnitExtensionPoint implements IExtensionPoint {
    private final List<JunitConfigurationElement> elements = new LinkedList<>();
    private final Map<String, JUnitExtension> extensions = new HashMap<>();

    /**
     * Default constructor.
     */
    public JUnitExtensionPoint() {
        super();
    }

    @Override
    public JunitConfigurationElement[] getConfigurationElements() throws InvalidRegistryObjectException {
        return elements.toArray(new JunitConfigurationElement[elements.size()]);
    }
    public void addConfigurationElement(final JunitConfigurationElement e) {
        elements.add(e);
    }
    @Override
    public String getNamespace() throws InvalidRegistryObjectException {
        return null;
    }
    @Override
    public String getNamespaceIdentifier() throws InvalidRegistryObjectException {
        return null;
    }
    @Override
    public IContributor getContributor() throws InvalidRegistryObjectException {
        return JUnitContributor.INSTANCE;
    }
    @Override
    public JUnitExtension getExtension(final String extensionId) throws InvalidRegistryObjectException {
        return extensions.get(extensionId);
    }
    @Override
    public JUnitExtension[] getExtensions() throws InvalidRegistryObjectException {
        final List<JUnitExtension> list = new LinkedList<>(extensions.values());
        return list.toArray(new JUnitExtension[list.size()]);
    }
    /**
     * @param id extension ID.
     * @param ext extension.
     */
    public void addExtension(final String id, final JUnitExtension ext) {
        extensions.put(id, ext);
    }

    @Override
    public String getLabel() throws InvalidRegistryObjectException {
        return null;
    }
    @Override
    public String getLabel(final String locale) throws InvalidRegistryObjectException {
        return null;
    }
    @Override
    public String getSchemaReference() throws InvalidRegistryObjectException {
        return null;
    }
    @Override
    public String getSimpleIdentifier() throws InvalidRegistryObjectException {
        return null;
    }
    @Override
    public String getUniqueIdentifier() throws InvalidRegistryObjectException {
        return null;
    }
    @Override
    public boolean isValid() {
        return true;
    }
}
