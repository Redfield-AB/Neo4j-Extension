/**
 *
 */
package se.redfield.knime.neo4j.connector.cfg;

import java.net.URI;
import java.net.URISyntaxException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorConfigSerializer {
    private static final String S_POOL_SIZE = "maxConnectionPoolSize";
    private static final String S_LOCATION = "location";
    //auth
    private static final String S_AUTH = "auth";

    private static final String S_SCHEME = "scheme";
    private static final String S_PRINCIPAL = "principal";
    private static final String S_CREDENTIALS = "credentials";

    //config
    private static final String ENC_KEY = "0=d#!s.1b";

    /**
     * Default constructor.
     */
    public ConnectorConfigSerializer() {
        super();
    }

    public void save(final ConnectorConfig config, final ConfigWO settings) {
        settings.addString(S_LOCATION, config.getLocation().toASCIIString());
        settings.addInt(S_POOL_SIZE, config.getMaxConnectionPoolSize());
        if (config.getAuth() != null) {
            saveAuth(config.getAuth(), settings.addConfig(S_AUTH));
        }
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
        } else {
            config.setAuth(null);
        }
        if (settings.containsKey(S_POOL_SIZE)) {
            config.setMaxConnectionPoolSize(settings.getInt(S_POOL_SIZE));
        }
        return config;
    }
    /**
     * @param auth authentication config.
     * @param settings settings.
     */
    private void saveAuth(final AuthConfig auth, final ConfigWO settings) {
        settings.addPassword(S_CREDENTIALS, ENC_KEY, auth.getCredentials());
        settings.addString(S_PRINCIPAL, auth.getPrincipal());
        settings.addString(S_SCHEME, auth.getScheme().name());
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
        auth.setPrincipal(settings.getString(S_PRINCIPAL));
        auth.setScheme(AuthScheme.valueOf(settings.getString(S_SCHEME)));
        return auth;
    }
}
