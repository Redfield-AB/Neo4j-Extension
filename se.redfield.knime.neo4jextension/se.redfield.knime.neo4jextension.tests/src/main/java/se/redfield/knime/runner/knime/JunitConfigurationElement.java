/**
 *
 */
package se.redfield.knime.runner.knime;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Status;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class JunitConfigurationElement implements IConfigurationElement {
    private final Map<String, String> attributes = new HashMap<>();

    /**
     * Default constructor.
     */
    public JunitConfigurationElement() {
        super();
    }

    @Override
    public Object createExecutableExtension(final String propertyName) throws CoreException {
        final String value = getAttribute(propertyName);
        try {
            return JunitConfigurationElement.class.getClassLoader().loadClass(value).newInstance();
        } catch (final Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, "junit", e.getMessage()));
        }
    }

    @Override
    public String getAttribute(final String name) throws InvalidRegistryObjectException {
        return attributes.get(name);
    }
    public void putAttribute(final String name, final String value) {
        attributes.put(name, value);
    }

    @Override
    public String getAttribute(final String attrName, final String locale) throws InvalidRegistryObjectException {
        return getAttribute(attrName);
    }

    @Override
    public String getAttributeAsIs(final String name) throws InvalidRegistryObjectException {
        return getAttribute(name);
    }

    @Override
    public String[] getAttributeNames() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IConfigurationElement[] getChildren() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IConfigurationElement[] getChildren(final String name) throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IExtension getDeclaringExtension() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getParent() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getValue() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getValue(final String locale) throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getValueAsIs() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
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
    public boolean isValid() {
        return true;
    }
}
