/**
 *
 */
package se.redfield.knime.runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
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
class UnitTestBundle implements Bundle, BundleContext {
    public static final UnitTestBundle INSTANCE = new UnitTestBundle();
    private Map<String, String> properties = new HashMap<>();
    private Dictionary<String, String> headers = new Hashtable<String, String>();

    private UnitTestBundle() {
        super();
    }

    @Override
    public int compareTo(final Bundle o) {
        return 0;
    }
    @Override
    public int getState() {
        return Bundle.RESOLVED;
    }
    @Override
    public void start(final int options) throws BundleException {
    }
    @Override
    public void start() throws BundleException {
    }
    @Override
    public void stop(final int options) throws BundleException {
    }
    @Override
    public void stop() throws BundleException {
    }
    @Override
    public void update(final InputStream input) throws BundleException {
    }
    @Override
    public void update() throws BundleException {
    }
    @Override
    public void uninstall() throws BundleException {
    }
    @Override
    public Dictionary<String, String> getHeaders() {
        return headers;
    }
    @Override
    public long getBundleId() {
        return 7777777L;
    }
    @Override
    public String getLocation() {
        return null;
    }
    @Override
    public ServiceReference<?>[] getRegisteredServices() {
        return null;
    }
    @Override
    public ServiceReference<?>[] getServicesInUse() {
        return null;
    }
    @Override
    public boolean hasPermission(final Object permission) {
        return true;
    }
    @Override
    public URL getResource(final String name) {
        return null;
    }
    @Override
    public Dictionary<String, String> getHeaders(final String locale) {
        return headers;
    }
    @Override
    public String getSymbolicName() {
        return "unittest-bundle";
    }
    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        return getClassLoader().loadClass(name);
    }
    private ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }
    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        return getClassLoader().getResources(name);
    }
    @Override
    public Enumeration<String> getEntryPaths(final String path) {
        return null;
    }
    @Override
    public URL getEntry(final String path) {
        return getClassLoader().getResource(path);
    }
    @Override
    public long getLastModified() {
        return 0;
    }
    @Override
    public Enumeration<URL> findEntries(final String path, final String filePattern, final boolean recurse) {
        return null;
    }
    @Override
    public BundleContext getBundleContext() {
        return this;
    }
    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(final int signersType) {
        return null;
    }
    @Override
    public Version getVersion() {
        return null;
    }
    @Override
    public <A> A adapt(final Class<A> type) {
        return null;
    }
    @Override
    public File getDataFile(final String filename) {
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
    public Filter createFilter(final String filter) throws InvalidSyntaxException {
        return null;
    }
    @Override
    public Bundle getBundle(final String location) {
        return this;
    }
}
