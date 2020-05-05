/**
 *
 */
package se.redfield.knime.neo4j.connector.cfg;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.neo4j.driver.Config.TrustStrategy;

import se.redfield.knime.neo4j.reader.cfg.SslTrustStrategy;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorConfigSerializer {
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
    private static final String ENC_KEY = "0=d#!s.1b";

    /**
     * Default constructor.
     */
    public ConnectorConfigSerializer() {
        super();
    }

    public void save(final ConnectorConfig config, final ConfigWO settings) {
        settings.addString(S_LOCATION, config.getLocation().toASCIIString());
        if (config.getAuth() != null) {
            saveAuth(config.getAuth(), settings.addConfig(S_AUTH));
        }
        saveConfig(config.getAdvancedSettings(), settings.addConfig(S_CONFIG));
    }
    public ConnectorConfig load(final ConfigRO settings) throws InvalidSettingsException {
        final ConnectorConfig config = new ConnectorConfig();
        try {
            config.setLocation(new URI(settings.getString(S_LOCATION)));
        } catch (final URISyntaxException e) {
            throw new InvalidSettingsException(e);
        }
        if (settings.containsKey(S_AUTH)) {
            config.setAuth(loadAuth(settings.getConfig(S_AUTH)));
        }
        if (settings.containsKey(S_CONFIG)) {
            config.setAdvancedSettings(loadConfig(settings.getConfig(S_CONFIG)));
        }
        return config;
    }
    /**
     * @param cfg Neo4j configuration.
     * @param settings target settings to save.
     */
    private void saveConfig(final AdvancedSettings cfg, final ConfigWO settings) {
        settings.addLong("connectionAcquisitionTimeoutMillis", cfg.getConnectionAcquisitionTimeoutMillis());
        settings.addLong("connectionTimeoutMillis", cfg.getConnectionTimeoutMillis());
        settings.addBoolean("encrypted", cfg.isEncrypted());
        settings.addInt("eventLoopThreads", cfg.getEventLoopThreads());
        settings.addLong("fetchSize", cfg.getFetchSize());
        settings.addLong("idleTimeBeforeConnectionTest", cfg.getIdleTimeBeforeConnectionTest());
        settings.addBoolean("isMetricsEnabled", cfg.isMetricsEnabled());
        settings.addBoolean("logLeakedSessions", cfg.isLogLeakedSessions());
        settings.addLong("maxConnectionLifetimeMillis", cfg.getMaxConnectionLifetimeMillis());
        settings.addInt("maxConnectionPoolSize", cfg.getMaxConnectionPoolSize());

        final SslTrustStrategy trustStrategy = cfg.getTrustStrategy();
        if (trustStrategy != null) {
            saveTrustStrategy(trustStrategy, settings.addConfig("trustStrategy"));
        }
    }
    /**
     * @param s trusted strategy.
     * @param settings setting to save to.
     */
    private void saveTrustStrategy(final SslTrustStrategy s, final ConfigWO settings) {
        settings.addString("strategy", s.getStrategy().name());
        if (s.getCertFile() != null) {
            settings.addString("certFile", s.getCertFile().getPath());
        }
        settings.addBoolean("isHostnameVerificationEnabled", s.isHostnameVerificationEnabled());
    }

    /**
     * @param auth authentication config.
     * @param settings settings.
     */
    private void saveAuth(final AuthConfig auth, final ConfigWO settings) {
        settings.addPassword(S_CREDENTIALS, ENC_KEY, auth.getCredentials());
        settings.addString(S_PARAMETERS, auth.getParameters());
        settings.addString(S_PRINCIPAL, auth.getPrincipal());
        settings.addString(S_REALM, auth.getRealm());
        settings.addString(S_SCHEME, auth.getScheme().name());
    }
    /**
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static AdvancedSettings loadConfig(final ConfigRO settings) throws InvalidSettingsException {
        final AdvancedSettings cfg = new AdvancedSettings();

        cfg.setConnectionAcquisitionTimeoutMillis(settings.getLong(
                "connectionAcquisitionTimeoutMillis"));
        cfg.setConnectionTimeoutMillis(settings.getLong("connectionTimeoutMillis"));
        cfg.setEncrypted(settings.getBoolean("encrypted"));
        cfg.setEventLoopThreads(settings.getInt("eventLoopThreads"));
        cfg.setFetchSize(settings.getLong("fetchSize"));
        cfg.setIdleTimeBeforeConnectionTest(settings.getLong("idleTimeBeforeConnectionTest"));
        cfg.setMetricsEnabled(settings.getBoolean("isMetricsEnabled"));
        cfg.setLogLeakedSessions(settings.getBoolean("logLeakedSessions"));
        cfg.setMaxConnectionLifetimeMillis(settings.getLong("maxConnectionLifetimeMillis"));
        cfg.setMaxConnectionPoolSize(settings.getInt("maxConnectionPoolSize"));

        if (settings.containsKey("trustStrategy")) {
            cfg.setTrustStrategy(loadTrustStrategy(settings.getConfig("trustStrategy")));
        }

        return cfg;
    }
    /**
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static SslTrustStrategy loadTrustStrategy(final ConfigRO settings) throws InvalidSettingsException {
        final SslTrustStrategy s = new SslTrustStrategy();
        s.setStrategy(TrustStrategy.Strategy.valueOf(settings.getString("strategy")));
        if (settings.containsKey("certFile")) {
            s.setCertFile(new File(settings.getString("certFile")));
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
        try {
            auth.setCredentials(settings.getPassword(S_CREDENTIALS, ENC_KEY));
        } catch (final Exception e) {
            //for backward compatibility
            auth.setCredentials(settings.getString(S_CREDENTIALS));
        }
        auth.setParameters(settings.getString(S_PARAMETERS));
        auth.setPrincipal(settings.getString(S_PRINCIPAL));
        auth.setRealm(settings.getString(S_REALM));
        auth.setScheme(AuthScheme.valueOf(settings.getString(S_SCHEME)));
        return auth;
    }
}
