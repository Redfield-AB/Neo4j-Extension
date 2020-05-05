/**
 *
 */
package se.redfield.knime.neo4j.connector.cfg;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.neo4j.driver.internal.async.pool.PoolSettings;
import org.neo4j.driver.internal.cluster.RoutingSettings;
import org.neo4j.driver.internal.handlers.pulln.FetchSizeUtil;
import org.neo4j.driver.internal.retry.RetrySettings;

import se.redfield.knime.neo4j.reader.cfg.SslTrustStrategy;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AdvancedSettings implements Cloneable {
    private int routingFailureLimit;
    private long routingRetryDelayMillis;
    private long routingTablePurgeDelayMillis;
    private long retrySettings;

    private SslTrustStrategy trustStrategy;
    private int maxConnectionPoolSize;
    private long maxConnectionLifetimeMillis;
    private boolean logLeakedSessions;
    private boolean isMetricsEnabled;
    private long idleTimeBeforeConnectionTest;
    private long fetchSize;
    private int eventLoopThreads;
    private boolean encrypted;
    private long connectionTimeoutMillis;
    private long connectionAcquisitionTimeoutMillis;

    public AdvancedSettings() {
        super();
        maxConnectionPoolSize = PoolSettings.DEFAULT_MAX_CONNECTION_POOL_SIZE;
        idleTimeBeforeConnectionTest = PoolSettings.DEFAULT_IDLE_TIME_BEFORE_CONNECTION_TEST;
        maxConnectionLifetimeMillis = PoolSettings.DEFAULT_MAX_CONNECTION_LIFETIME;
        connectionAcquisitionTimeoutMillis = PoolSettings.DEFAULT_CONNECTION_ACQUISITION_TIMEOUT;

        maxConnectionPoolSize = PoolSettings.DEFAULT_MAX_CONNECTION_POOL_SIZE;
        idleTimeBeforeConnectionTest = PoolSettings.DEFAULT_IDLE_TIME_BEFORE_CONNECTION_TEST;
        maxConnectionLifetimeMillis = PoolSettings.DEFAULT_MAX_CONNECTION_LIFETIME;
        connectionAcquisitionTimeoutMillis = PoolSettings.DEFAULT_CONNECTION_ACQUISITION_TIMEOUT;
        routingFailureLimit = RoutingSettings.DEFAULT.maxRoutingFailures();
        routingRetryDelayMillis = RoutingSettings.DEFAULT.retryTimeoutDelay();
        routingTablePurgeDelayMillis = RoutingSettings.DEFAULT.routingTablePurgeDelayMs();
        connectionTimeoutMillis = (int) TimeUnit.SECONDS.toMillis( 30 );
        retrySettings = RetrySettings.DEFAULT.maxRetryTimeMs();
        isMetricsEnabled = false;
        fetchSize = FetchSizeUtil.DEFAULT_FETCH_SIZE;
        eventLoopThreads = 1;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AdvancedSettings)) {
            return false;
        }

        final AdvancedSettings that = (AdvancedSettings) obj;
        return
                Objects.equals(getConnectionAcquisitionTimeoutMillis(), that.getConnectionAcquisitionTimeoutMillis())
                && Objects.equals(getConnectionTimeoutMillis(), that.getConnectionTimeoutMillis())
                && Objects.equals(isEncrypted(), that.isEncrypted())
                && Objects.equals(getEventLoopThreads(), that.getEventLoopThreads())
                && Objects.equals(getFetchSize(), that.getFetchSize())
                && Objects.equals(getIdleTimeBeforeConnectionTest(), that.getIdleTimeBeforeConnectionTest())
                && Objects.equals(isMetricsEnabled(), that.isMetricsEnabled())
                && Objects.equals(isLogLeakedSessions(), that.isLogLeakedSessions())
                && Objects.equals(getMaxConnectionLifetimeMillis(), that.getMaxConnectionLifetimeMillis())
                && Objects.equals(getMaxConnectionPoolSize(), that.getMaxConnectionPoolSize())
                && Objects.equals(getRoutingFailureLimit(), that.getRoutingFailureLimit())
                && Objects.equals(getRoutingRetryDelayMillis(), that.getRoutingRetryDelayMillis())
                && Objects.equals(getRoutingTablePurgeDelayMillis(), that.getRoutingTablePurgeDelayMillis())
                && Objects.equals(getRetrySettings(), that.getRetrySettings())
                && Objects.equals(trustStrategy(), that.trustStrategy());
    }

    private SslTrustStrategy trustStrategy() {
        return trustStrategy;
    }
    public int getMaxConnectionPoolSize() {
        return maxConnectionPoolSize;
    }
    public void setMaxConnectionPoolSize(final int maxConnectionPoolSize) {
        this.maxConnectionPoolSize = maxConnectionPoolSize;
    }
    public long getMaxConnectionLifetimeMillis() {
        return maxConnectionLifetimeMillis;
    }
    public int getRoutingFailureLimit() {
        return routingFailureLimit;
    }
    public void setMaxConnectionLifetimeMillis(final long maxConnectionLifetimeMillis) {
        this.maxConnectionLifetimeMillis = maxConnectionLifetimeMillis;
    }
    public boolean isLogLeakedSessions() {
        return logLeakedSessions;
    }
    public void setLogLeakedSessions(final boolean logLeakedSessions) {
        this.logLeakedSessions = logLeakedSessions;
    }
    public boolean isMetricsEnabled() {
        return isMetricsEnabled;
    }
    public void setMetricsEnabled(final boolean isMetricsEnabled) {
        this.isMetricsEnabled = isMetricsEnabled;
    }
    public long getIdleTimeBeforeConnectionTest() {
        return idleTimeBeforeConnectionTest;
    }
    public void setIdleTimeBeforeConnectionTest(final long idleTimeBeforeConnectionTest) {
        this.idleTimeBeforeConnectionTest = idleTimeBeforeConnectionTest;
    }
    public long getFetchSize() {
        return fetchSize;
    }
    public void setFetchSize(final long fetchSize) {
        this.fetchSize = fetchSize;
    }
    public int getEventLoopThreads() {
        return eventLoopThreads;
    }
    public void setEventLoopThreads(final int eventLoopThreads) {
        this.eventLoopThreads = eventLoopThreads;
    }
    public boolean isEncrypted() {
        return encrypted;
    }
    public void setEncrypted(final boolean encrypted) {
        this.encrypted = encrypted;
    }
    public long getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }
    public void setConnectionTimeoutMillis(final long connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }
    public long getConnectionAcquisitionTimeoutMillis() {
        return connectionAcquisitionTimeoutMillis;
    }
    public void setConnectionAcquisitionTimeoutMillis(final long connectionAcquisitionTimeoutMillis) {
        this.connectionAcquisitionTimeoutMillis = connectionAcquisitionTimeoutMillis;
    }
    public void setRoutingFailureLimit(final int routingFailureLimit) {
        this.routingFailureLimit = routingFailureLimit;
    }

    @Override
    public int hashCode() {
        return
            Objects.hash(getConnectionAcquisitionTimeoutMillis(),
            getConnectionTimeoutMillis(),
            isEncrypted(),
            getEventLoopThreads(),
            getFetchSize(),
            getIdleTimeBeforeConnectionTest(),
            isMetricsEnabled(),
            isLogLeakedSessions(),
            getMaxConnectionLifetimeMillis(),
            getMaxConnectionPoolSize(),
            getRoutingFailureLimit(),
            getRoutingRetryDelayMillis(),
            getRoutingTablePurgeDelayMillis(),
            getRetrySettings(),
            trustStrategy());
    }
    public long getRoutingRetryDelayMillis() {
        return routingRetryDelayMillis;
    }
    public void setRoutingRetryDelayMillis(final long routingRetryDelayMillis) {
        this.routingRetryDelayMillis = routingRetryDelayMillis;
    }
    public long getRoutingTablePurgeDelayMillis() {
        return routingTablePurgeDelayMillis;
    }
    public void setRoutingTablePurgeDelayMillis(final long routingTablePurgeDelayMillis) {
        this.routingTablePurgeDelayMillis = routingTablePurgeDelayMillis;
    }
    public long getRetrySettings() {
        return retrySettings;
    }
    public void setRetrySettings(final long retrySettings) {
        this.retrySettings = retrySettings;
    }
    public SslTrustStrategy getTrustStrategy() {
        return trustStrategy;
    }
    public void setTrustStrategy(final SslTrustStrategy trustStrategy) {
        this.trustStrategy = trustStrategy;
    }
    @Override
    public AdvancedSettings clone() {
        try {
            final AdvancedSettings clone = (AdvancedSettings) super.clone();
            clone.trustStrategy = trustStrategy == null ? null : trustStrategy.clone();
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
