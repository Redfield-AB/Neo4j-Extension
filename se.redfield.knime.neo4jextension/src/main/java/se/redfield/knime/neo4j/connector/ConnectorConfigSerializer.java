/**
 *
 */
package se.redfield.knime.neo4j.connector;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorConfigSerializer {
    private static final String S_POOL_SIZE = "maxConnectionPoolSize";
    private static final String S_LOCATION = "location";
    private static final String S_DATABASE = "database";
    private static final String USED_DEFAULT_DB_NAME = "usedDefaultDbName";
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
        settings.addString(S_DATABASE, config.getDatabase());
        settings.addString(USED_DEFAULT_DB_NAME, String.valueOf(config.isUsedDefaultDbName()));
        settings.addInt(S_POOL_SIZE, config.getMaxConnectionPoolSize());
        if (config.getAuth() != null) {
            saveAuth(config.getAuth(), settings.addConfig(S_AUTH), config.getOauthToken());
        }
    }

    public ConnectorConfig load(final ConfigRO settings) throws InvalidSettingsException {
        final ConnectorConfig config = new ConnectorConfig();
        try {
            config.setLocation(new URI(settings.getString(S_LOCATION)));
        } catch (final URISyntaxException e) {
            throw new InvalidSettingsException(e);
        }

        config.setDatabase(settings.getString(S_DATABASE, "neo4j"));
        config.setUsedDefaultDbName(Boolean.parseBoolean(settings.getString(USED_DEFAULT_DB_NAME, "true")));

        if (settings.containsKey(S_AUTH)) {
            AuthConfig loadedAuth = loadAuth(settings.getConfig(S_AUTH));
            config.setAuth(loadedAuth);
            // If the loaded scheme is OAuth2, the token is now in loadedAuth.getCredentials()
            if (loadedAuth.getScheme() == AuthScheme.OAuth2) {
                config.setOauthToken(loadedAuth.getCredentials());
            }
        } else {
            // If no auth settings are present, initialize with default basic scheme
            config.setAuth(new AuthConfig());
            config.getAuth().setScheme(AuthScheme.basic);
            config.getAuth().setPrincipal("neo4j");
        }
        if (settings.containsKey(S_POOL_SIZE)) {
            config.setMaxConnectionPoolSize(settings.getInt(S_POOL_SIZE));
        }
        return config;
    }

    /**
     * @param auth authentication config.
     * @param settings settings.
     * @param oauthToken The OAuth2 token to save if the scheme is OAuth2.
     */
    private void saveAuth(final AuthConfig auth, final ConfigWO settings, final String oauthToken) {
        if (auth.getScheme() == AuthScheme.basic) {
            settings.addPassword(S_CREDENTIALS, ENC_KEY, auth.getCredentials());
        } else if (auth.getScheme() == AuthScheme.OAuth2) {
            // Save OAuth2 token in the credentials field
            if (oauthToken != null) {
                settings.addPassword(S_CREDENTIALS, ENC_KEY, oauthToken);
            }
        }
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
        auth.setPrincipal(settings.getString(S_PRINCIPAL));
        AuthScheme scheme = AuthScheme.valueOf(settings.getString(S_SCHEME));
        auth.setScheme(scheme);

        if (scheme == AuthScheme.basic) {
            try {
                auth.setCredentials(settings.getPassword(S_CREDENTIALS, ENC_KEY));
            } catch (final Exception e) {
                //for backward compatibility, ensure credentials are not null
                auth.setCredentials(settings.getString(S_CREDENTIALS, ""));
            }
        } else if (scheme == AuthScheme.OAuth2) {
            // Load OAuth2 token from the credentials field
            try {
                auth.setCredentials(settings.getPassword(S_CREDENTIALS, ENC_KEY));
            } catch (final Exception e) {
                // For backward compatibility, if loading as password fails, try loading as string
                auth.setCredentials(settings.getString(S_CREDENTIALS, ""));
            }
        }
        return auth;
    }
}
