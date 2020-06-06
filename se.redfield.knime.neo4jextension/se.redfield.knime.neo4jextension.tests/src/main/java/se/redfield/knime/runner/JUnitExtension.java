/**
 *
 */
package se.redfield.knime.runner;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class JUnitExtension implements IExtension {
    private List<JunitConfigurationElement> elements = new LinkedList<>();

    public JUnitExtension() {
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
    public String getExtensionPointUniqueIdentifier() throws InvalidRegistryObjectException {
        return null;
    }
    @Override
    public String getLabel() throws InvalidRegistryObjectException {
        return null;
    }
    @Override
    public String getLabel(final String locale) throws InvalidRegistryObjectException {
        return getLabel();
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
