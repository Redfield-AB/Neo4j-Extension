/**
 *
 */
package se.redfield.knime.neo4j.reader.cfg;

import java.io.File;
import java.util.Objects;

import org.neo4j.driver.Config.TrustStrategy.Strategy;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class SslTrustStrategy {
    private Strategy strategy;
    private File certFile;
    private boolean hostnameVerificationEnabled = true;

    public SslTrustStrategy() {
        super();
    }

    public Strategy getStrategy() {
        return strategy;
    }
    public void setStrategy(final Strategy strategy) {
        this.strategy = strategy;
    }
    public File getCertFile() {
        return certFile;
    }
    public void setCertFile(final File certFile) {
        this.certFile = certFile;
    }
    public boolean isHostnameVerificationEnabled() {
        return hostnameVerificationEnabled;
    }
    public void setHostnameVerificationEnabled(final boolean hostnameVerificationEnabled) {
        this.hostnameVerificationEnabled = hostnameVerificationEnabled;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof SslTrustStrategy)) {
            return false;
        }

        final SslTrustStrategy that = (SslTrustStrategy) obj;
        return Objects.equals(getCertFile(), that.getCertFile())
            && Objects.equals(isHostnameVerificationEnabled(), that.isHostnameVerificationEnabled())
            && Objects.equals(getStrategy(), that.getStrategy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCertFile(),
                isHostnameVerificationEnabled(),
                getStrategy());
    }
}
