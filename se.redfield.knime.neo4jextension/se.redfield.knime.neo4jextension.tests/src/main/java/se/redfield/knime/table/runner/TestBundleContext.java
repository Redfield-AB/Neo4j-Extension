/**
 *
 */
package se.redfield.knime.table.runner;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;

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
public class TestBundleContext implements BundleContext {

    /**
     *
     */
    public TestBundleContext() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getProperty(final String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getBundle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle installBundle(final String location, final InputStream input) throws BundleException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle installBundle(final String location) throws BundleException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getBundle(final long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addServiceListener(final ServiceListener listener, final String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addServiceListener(final ServiceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeServiceListener(final ServiceListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addBundleListener(final BundleListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeBundleListener(final BundleListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addFrameworkListener(final FrameworkListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeFrameworkListener(final FrameworkListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public ServiceRegistration<?> registerService(final String[] clazzes, final Object service, final Dictionary<String, ?> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceRegistration<?> registerService(final String clazz, final Object service, final Dictionary<String, ?> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(final Class<S> clazz, final S service, final Dictionary<String, ?> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(final Class<S> clazz, final ServiceFactory<S> factory,
            final Dictionary<String, ?> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference<?> getServiceReference(final String clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(final Class<S> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(final Class<S> clazz, final String filter)
            throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S> S getService(final ServiceReference<S> reference) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean ungetService(final ServiceReference<?> reference) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(final ServiceReference<S> reference) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getDataFile(final String filename) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Filter createFilter(final String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getBundle(final String location) {
        // TODO Auto-generated method stub
        return null;
    }
}
