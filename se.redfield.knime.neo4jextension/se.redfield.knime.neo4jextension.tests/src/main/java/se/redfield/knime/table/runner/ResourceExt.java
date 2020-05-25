/**
 *
 */
package se.redfield.knime.table.runner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@SuppressWarnings("restriction")
public class ResourceExt extends sun.misc.Resource {
    private final sun.misc.Resource delegate;
    public ResourceExt(final sun.misc.Resource delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
    @Override
    public URL getURL() {
        return delegate.getURL();
    }
    @Override
    public URL getCodeSourceURL() {
        return delegate.getCodeSourceURL();
    }
    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }
    @Override
    public int getContentLength() throws IOException {
        return delegate.getContentLength();
    }
}
