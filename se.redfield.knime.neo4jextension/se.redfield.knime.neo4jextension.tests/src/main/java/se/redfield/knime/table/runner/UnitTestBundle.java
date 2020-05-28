/**
 *
 */
package se.redfield.knime.table.runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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
import org.osgi.framework.Version;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class UnitTestBundle implements Bundle, BundleContext {
    public static final UnitTestBundle INSTANCE = new UnitTestBundle();
    private Map<String, String> properties = new HashMap<>();

    private UnitTestBundle() {
        super();
    }

    @Override
    public int compareTo(final Bundle o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getState() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void start(final int options) throws BundleException {
        // TODO Auto-generated method stub

    }

    @Override
    public void start() throws BundleException {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop(final int options) throws BundleException {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() throws BundleException {
        // TODO Auto-generated method stub

    }

    @Override
    public void update(final InputStream input) throws BundleException {
        // TODO Auto-generated method stub

    }

    @Override
    public void update() throws BundleException {
        // TODO Auto-generated method stub

    }

    @Override
    public void uninstall() throws BundleException {
        // TODO Auto-generated method stub

    }

    @Override
    public Dictionary<String, String> getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getBundleId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference<?>[] getServicesInUse() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasPermission(final Object permission) {
        return true;
    }

    @Override
    public URL getResource(final String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Dictionary<String, String> getHeaders(final String locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSymbolicName() {
        return "unittest-bundle";
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        return getClass().getClassLoader().loadClass(name);
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<String> getEntryPaths(final String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getEntry(final String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLastModified() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Enumeration<URL> findEntries(final String path, final String filePattern, final boolean recurse) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleContext getBundleContext() {
        return this;
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(final int signersType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Version getVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <A> A adapt(final Class<A> type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getDataFile(final String filename) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProperty(final String key) {
        return properties.get(key);
    }
    @Override
    public Bundle getBundle() {
        return this;
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
        return this;
    }
    @Override
    public Bundle[] getBundles() {
        return new Bundle[] {this};
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
    public Filter createFilter(final String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getBundle(final String location) {
        return this;
    }
}
