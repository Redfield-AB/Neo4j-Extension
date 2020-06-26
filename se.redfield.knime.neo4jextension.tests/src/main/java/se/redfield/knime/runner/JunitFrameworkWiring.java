/**
 *
 */
package se.redfield.knime.runner;

import java.util.Collection;
import java.util.LinkedList;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class JunitFrameworkWiring implements FrameworkWiring {
    /**
     * Default constructor.
     */
    public JunitFrameworkWiring() {
        super();
    }

    @Override
    public Bundle getBundle() {
        return UnitTestBundle.INSTANCE;
    }
    @Override
    public void refreshBundles(final Collection<Bundle> bundles, final FrameworkListener... listeners) {
    }
    @Override
    public boolean resolveBundles(final Collection<Bundle> bundles) {
        return true;
    }
    @Override
    public Collection<Bundle> getRemovalPendingBundles() {
        return new LinkedList<>();
    }
    @Override
    public Collection<Bundle> getDependencyClosure(final Collection<Bundle> bundles) {
        return new LinkedList<>();
    }
    @Override
    public Collection<BundleCapability> findProviders(final Requirement requirement) {
        return new LinkedList<>();
    }
}
