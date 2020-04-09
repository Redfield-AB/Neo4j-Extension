/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.neo4j.driver.Config;
import org.neo4j.driver.Config.TrustStrategy;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JConfig {
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
    public Neo4JConfig() {
        super();
    }

    /**
     * @param settings settings storage.
     */
    public void saveTo(final NodeSettingsWO settings) {
        settings.addString(S_LOCATION, location.toASCIIString());
        if (auth != null) {
            saveAuth(auth, settings.addNodeSettings(S_AUTH));
        }
        if (config != null) {
            saveConfig(config, settings.addNodeSettings(S_CONFIG));
        }
    }
    /**
     * @param cfg Neo4J configuration.
     * @param settings target settings to save.
     */
    private void saveConfig(final Config cfg, final NodeSettingsWO settings) {
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

        TrustStrategy trustStrategy = cfg.trustStrategy();
        if (trustStrategy != null) {
            saveTrustStrategy(trustStrategy, settings.addNodeSettings("trustStrategy"));
        }
    }
    /**
     * @param s trusted strategy.
     * @param settings setting to save to.
     */
    private void saveTrustStrategy(final TrustStrategy s, final NodeSettingsWO settings) {
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
    private void saveAuth(final AuthConfig auth, final NodeSettingsWO settings) {
        settings.addString(S_CREDENTIALS, auth.getCredentials());
        settings.addString(S_PARAMETERS, auth.getParameters());
        settings.addString(S_PRINCIPAL, auth.getPrincipal());
        settings.addString(S_REALM, auth.getRealm());
        settings.addString(S_SCHEME, auth.getScheme());
    }
    /**
     * @param settings settings to load.
     * @return Neo4J connector configuration.
     * @throws InvalidSettingsException
     */
    public static Neo4JConfig load(final NodeSettingsRO settings) throws InvalidSettingsException {
        Neo4JConfig cfg = new Neo4JConfig();
        try {
            cfg.location = new URI(settings.getString(S_LOCATION));
        } catch (URISyntaxException e) {
            throw new InvalidSettingsException(e);
        }
        if (settings.containsKey(S_AUTH)) {
            cfg.auth = loadAuth(settings.getNodeSettings(S_AUTH));
        }
        if (settings.containsKey(S_CONFIG)) {
            cfg.config = loadConfig(settings.getNodeSettings(S_CONFIG));
        }
        return cfg;
    }

    /**
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static Config loadConfig(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config.ConfigBuilder cfg = Config.builder();

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
            cfg.withTrustStrategy(loadTrustStrategy(settings.getNodeSettings("trustStrategy")));
        }

        return cfg.build();
    }

    /**
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static TrustStrategy loadTrustStrategy(final NodeSettingsRO settings) throws InvalidSettingsException {
        TrustStrategy.Strategy strategy = TrustStrategy.Strategy.valueOf(
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
                String certFile = settings.getString("certFile");

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
    private static AuthConfig loadAuth(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        AuthConfig auth = new AuthConfig();
        auth.setCredentials(settings.getString(S_CREDENTIALS));
        auth.setParameters(settings.getString(S_PARAMETERS));
        auth.setPrincipal(settings.getString(S_PRINCIPAL));
        auth.setRealm(settings.getString(S_REALM));
        auth.setScheme(settings.getString(S_SCHEME));
        return auth;
    }

    /**
     * @param settings settings to validate.
     */
    public static void validate(final NodeSettingsRO settings) {
        //TODO
    }
    /**
     * @return default settings.
     */
    public static Neo4JConfig createDefault() {
        Neo4JConfig cfg = new Neo4JConfig();
        try {
            cfg.location = new URI("bolt://localhost:7687");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }
}
