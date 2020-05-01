/**
 *
 */
package se.redfield.knime.junit;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class MockBundleContext implements BundleContext {
    private Map<String, String> props = new HashMap<>();
    @Override
    public String getProperty(final String key) {
        return props.get(key);
    }
    public void setProperty(final String key, final String value) {
        props.put(key, value);
    }
    @Override
    public Bundle getBundle() {
        return null;
    }
    @Override
    public Bundle installBundle(final String location, final InputStream input) throws BundleException {
        return null;
    }
    @Override
    public Bundle installBundle(final String location) throws BundleException {
        return null;
    }
    @Override
    public Bundle getBundle(final long id) {
        return null;
    }
    @Override
    public Bundle[] getBundles() {
        return null;
    }
    @Override
    public void addServiceListener(final ServiceListener listener, final String filter) throws InvalidSyntaxException {
    }
    @Override
    public void addServiceListener(final ServiceListener listener) {
    }
    @Override
    public void removeServiceListener(final ServiceListener listener) {
    }
    @Override
    public void addBundleListener(final BundleListener listener) {
    }
    @Override
    public void removeBundleListener(final BundleListener listener) {
    }
    @Override
    public void addFrameworkListener(final FrameworkListener listener) {
    }
    @Override
    public void removeFrameworkListener(final FrameworkListener listener) {
    }
    @Override
    public ServiceRegistration<?> registerService(final String[] clazzes, final Object service, final Dictionary<String, ?> properties) {
        return null;
    }
    @Override
    public ServiceRegistration<?> registerService(final String clazz, final Object service, final Dictionary<String, ?> properties) {
        return null;
    }
    @Override
    public <S> ServiceRegistration<S> registerService(final Class<S> clazz, final S service, final Dictionary<String, ?> properties) {
        return null;
    }
    @Override
    public <S> ServiceRegistration<S> registerService(final Class<S> clazz, final ServiceFactory<S> factory,
            final Dictionary<String, ?> properties) {
        return null;
    }
    @Override
    public ServiceReference<?>[] getServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException {
        return null;
    }
    @Override
    public ServiceReference<?>[] getAllServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException {
        return null;
    }
    @Override
    public ServiceReference<?> getServiceReference(final String clazz) {
        return null;
    }
    @Override
    public <S> ServiceReference<S> getServiceReference(final Class<S> clazz) {
        return null;
    }
    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(final Class<S> clazz, final String filter)
            throws InvalidSyntaxException {
        return null;
    }
    @Override
    public <S> S getService(final ServiceReference<S> reference) {
        return null;
    }
    @Override
    public boolean ungetService(final ServiceReference<?> reference) {
        return false;
    }
    @Override
    public <S> ServiceObjects<S> getServiceObjects(final ServiceReference<S> reference) {
        return null;
    }
    @Override
    public File getDataFile(final String filename) {
        return null;
    }
    @Override
    public Filter createFilter(final String filter) throws InvalidSyntaxException {
        return null;
    }
    @Override
    public Bundle getBundle(final String location) {
        return null;
    }
}
