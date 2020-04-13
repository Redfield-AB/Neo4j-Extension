/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.neo4j.driver.Config;
import org.neo4j.driver.Config.TrustStrategy;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JConnector {
    private static final String S_LOCATION = "location";
    //auth
    private static final String S_AUTH = "auth";

    private static final String S_SCHEME = "scheme";
    private static final String S_REALM = "realm";
    private static final String S_PRINCIPAL = "principal";
    private static final String S_PARAMETERS = "parameters";
    private static final String S_CREDENTIALS = "credentials";

    //config
    private static final String S_CONFIG = "config";

    private URI location;
    private AuthConfig auth;
    private Config config;

    /**
     * Default constructor.
     */
    public Neo4JConnector() {
        super();
        reset();
    }

    public void save(final ConfigWO settings) {
        settings.addString(S_LOCATION, location.toASCIIString());
        if (auth != null) {
            saveAuth(auth, settings.addConfig(S_AUTH));
        }
        saveConfig(config, settings.addConfig(S_CONFIG));
    }
    public void load(final ConfigRO settings) throws InvalidSettingsException {
        try {
            this.location = new URI(settings.getString(S_LOCATION));
        } catch (final URISyntaxException e) {
            throw new InvalidSettingsException(e);
        }
        if (settings.containsKey(S_AUTH)) {
            this.auth = loadAuth(settings.getConfig(S_AUTH));
        }
        if (settings.containsKey(S_CONFIG)) {
            this.config = loadConfig(settings.getConfig(S_CONFIG));
        }
    }
    public void reset() {
        this.config = Config.builder().withEventLoopThreads(1).build();
        this.auth = null;
        try {
            this.location = new URI("bolt://localhost:7687");
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * @param cfg Neo4J configuration.
     * @param settings target settings to save.
     */
    private void saveConfig(final Config cfg, final ConfigWO settings) {
        settings.addLong("connectionAcquisitionTimeoutMillis", cfg.connectionAcquisitionTimeoutMillis());
        settings.addInt("connectionTimeoutMillis", cfg.connectionTimeoutMillis());
        settings.addBoolean("encrypted", cfg.encrypted());
        settings.addInt("eventLoopThreads", cfg.eventLoopThreads());
        settings.addLong("fetchSize", cfg.fetchSize());
        settings.addLong("idleTimeBeforeConnectionTest", cfg.idleTimeBeforeConnectionTest());
        settings.addBoolean("isMetricsEnabled", cfg.isMetricsEnabled());
        settings.addBoolean("logLeakedSessions", cfg.logLeakedSessions());
        settings.addLong("maxConnectionLifetimeMillis", cfg.maxConnectionLifetimeMillis());
        settings.addInt("maxConnectionPoolSize", cfg.maxConnectionPoolSize());

        final TrustStrategy trustStrategy = cfg.trustStrategy();
        if (trustStrategy != null) {
            saveTrustStrategy(trustStrategy, settings.addConfig("trustStrategy"));
        }
    }
    /**
     * @param s trusted strategy.
     * @param settings setting to save to.
     */
    private void saveTrustStrategy(final TrustStrategy s, final ConfigWO settings) {
        settings.addString("strategy", s.strategy().name());
        if (s.certFile() != null) {
            settings.addString("certFile", s.certFile().getPath());
        }
        settings.addBoolean("isHostnameVerificationEnabled", s.isHostnameVerificationEnabled());
    }

    /**
     * @param auth authentication config.
     * @param settings settings.
     */
    private void saveAuth(final AuthConfig auth, final ConfigWO settings) {
        settings.addString(S_CREDENTIALS, auth.getCredentials());
        settings.addString(S_PARAMETERS, auth.getParameters());
        settings.addString(S_PRINCIPAL, auth.getPrincipal());
        settings.addString(S_REALM, auth.getRealm());
        settings.addString(S_SCHEME, auth.getScheme());
    }
    /**
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static Config loadConfig(final ConfigRO settings) throws InvalidSettingsException {
        final Config.ConfigBuilder cfg = Config.builder();

        cfg.withConnectionAcquisitionTimeout(settings.getLong(
                "connectionAcquisitionTimeoutMillis"), TimeUnit.MILLISECONDS);
        cfg.withConnectionTimeout(settings.getInt("connectionTimeoutMillis"), TimeUnit.MILLISECONDS);
        if (settings.getBoolean("encrypted")) {
            cfg.withEncryption();
        } else {
            cfg.withoutEncryption();
        }
        cfg.withEventLoopThreads(settings.getInt("eventLoopThreads"));
        cfg.withFetchSize(settings.getLong("fetchSize"));
        cfg.withConnectionLivenessCheckTimeout(
                settings.getLong("idleTimeBeforeConnectionTest"), TimeUnit.MILLISECONDS);
        if (settings.getBoolean("isMetricsEnabled")) {
            cfg.withDriverMetrics();
        } else {
            cfg.withoutDriverMetrics();
        }
        if (settings.getBoolean("logLeakedSessions")) {
            cfg.withLeakedSessionsLogging();
        }
        cfg.withMaxConnectionLifetime(settings.getLong("maxConnectionLifetimeMillis"),
                TimeUnit.MILLISECONDS);
        cfg.withMaxConnectionPoolSize(settings.getInt("maxConnectionPoolSize"));

        if (settings.containsKey("trustStrategy")) {
            cfg.withTrustStrategy(loadTrustStrategy(settings.getConfig("trustStrategy")));
        }

        return cfg.build();
    }

    /**
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static TrustStrategy loadTrustStrategy(final ConfigRO settings) throws InvalidSettingsException {
        final TrustStrategy.Strategy strategy = TrustStrategy.Strategy.valueOf(
                settings.getString("strategy"));

        TrustStrategy s;
        switch (strategy) {
            case TRUST_ALL_CERTIFICATES:
                s = TrustStrategy.trustAllCertificates();
                break;
            case TRUST_SYSTEM_CA_SIGNED_CERTIFICATES:
                s = TrustStrategy.trustSystemCertificates();
                break;
            case TRUST_CUSTOM_CA_SIGNED_CERTIFICATES:
                final String certFile = settings.getString("certFile");

                s = TrustStrategy.trustCustomCertificateSignedBy(new File(certFile));
                break;
                default:
                    throw new InvalidSettingsException("Unexpected trust strategy: "
                            + strategy);

        }
        return s;
    }
    /**
     * @param settings
     * @return auth configuration.
     * @throws InvalidSettingsException
     */
    private static AuthConfig loadAuth(final ConfigRO settings)
            throws InvalidSettingsException {
        final AuthConfig auth = new AuthConfig();
        auth.setCredentials(settings.getString(S_CREDENTIALS));
        auth.setParameters(settings.getString(S_PARAMETERS));
        auth.setPrincipal(settings.getString(S_PRINCIPAL));
        auth.setRealm(settings.getString(S_REALM));
        auth.setScheme(settings.getString(S_SCHEME));
        return auth;
    }

    public URI getLocation() {
        return location;
    }
    public void setLocation(final URI location) {
        this.location = location;
    }
    public AuthConfig getAuth() {
        return auth;
    }
    public void setAuth(final AuthConfig auth) {
        this.auth = auth;
    }
    public Config getConfig() {
        return config;
    }
    public void setConfig(final Config config) {
        this.config = config;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Neo4JConnector)) {
            return false;
        }

        final Neo4JConnector that = (Neo4JConnector) obj;
        return Objects.equals(this.location, that.location)
            && Objects.equals(this.auth, that.auth)
            && configsEquals(this.config, that.config);
    }
    /**
     * @param c1 first config.
     * @param c2 second config.
     * @return true if equals.
     */
    private boolean configsEquals(final Config c1, final Config c2) {
        return
            Objects.equals(c1.connectionAcquisitionTimeoutMillis(), c2.connectionAcquisitionTimeoutMillis())
            && Objects.equals(c1.connectionTimeoutMillis(), c2.connectionTimeoutMillis())
            && Objects.equals(c1.encrypted(), c2.encrypted())
            && Objects.equals(c1.eventLoopThreads(), c2.eventLoopThreads())
            && Objects.equals(c1.fetchSize(), c2.fetchSize())
            && Objects.equals(c1.idleTimeBeforeConnectionTest(), c2.idleTimeBeforeConnectionTest())
            && Objects.equals(c1.isMetricsEnabled(), c2.isMetricsEnabled())
            && Objects.equals(c1.logLeakedSessions(), c2.logLeakedSessions())
            && Objects.equals(c1.maxConnectionLifetimeMillis(), c2.maxConnectionLifetimeMillis())
            && Objects.equals(c1.maxConnectionPoolSize(), c2.maxConnectionPoolSize())
            && strategiesEquals(c1.trustStrategy(), c2.trustStrategy());
    }

    /**
     * @param t1 first strategy.
     * @param t2 second strategy.
     * @return true if equals.
     */
    private boolean strategiesEquals(final TrustStrategy t1, final TrustStrategy t2) {
        return Objects.equals(t1.certFile(), t2.certFile())
            && Objects.equals(t1.isHostnameVerificationEnabled(), t2.isHostnameVerificationEnabled())
            && Objects.equals(t1.strategy(), t2.strategy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.location,
                this.auth,
                configHash(this.config));
    }
    /**
     * @param c config.
     * @return config hash
     */
    private int configHash(final Config c) {
        return
            Objects.hash(c.connectionAcquisitionTimeoutMillis(),
            c.connectionTimeoutMillis(),
            c.encrypted(),
            c.eventLoopThreads(),
            c.fetchSize(),
            c.idleTimeBeforeConnectionTest(),
            c.isMetricsEnabled(),
            c.logLeakedSessions(),
            c.maxConnectionLifetimeMillis(),
            c.maxConnectionPoolSize(),
            strategiesHash(c.trustStrategy()));
    }
    /**
     * @param strategy trust strategy.
     * @return hash.
     */
    private int strategiesHash(final TrustStrategy strategy) {
        return Objects.hash(strategy.certFile(),
            strategy.isHostnameVerificationEnabled(),
            strategy.strategy());
    }
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NeoJ4 DB: ");
        sb.append(getLocation());
        return sb.toString();
    }
}
