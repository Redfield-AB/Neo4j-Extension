/**
 *
 */
package se.redfield.knime.neo4j.connector;

import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorConfig implements Cloneable {
    private URI location;
    private AuthConfig auth;
    private int maxConnectionPoolSize;
    private String database;
    private boolean usedDefaultDbName;
    private String oauthToken; // New field to store the resolved OAuth2 token

    public ConnectorConfig() {
        super();
        maxConnectionPoolSize = Math.max(Runtime.getRuntime().availableProcessors(), 1);
        database = "neo4j";
        usedDefaultDbName = true;

        try {
            this.location = new URI("bolt://localhost:7687");
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }

        auth = new AuthConfig();
        auth.setScheme(AuthScheme.basic); // Revert to basic as default
        auth.setPrincipal("neo4j");
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

    public int getMaxConnectionPoolSize() {
        return maxConnectionPoolSize;
    }

    public void setMaxConnectionPoolSize(final int maxConnectionPoolSize) {
        this.maxConnectionPoolSize = maxConnectionPoolSize;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public boolean isUsedDefaultDbName() {
        return usedDefaultDbName;
    }

    public void setUsedDefaultDbName(boolean usedDefaultDbName) {
        this.usedDefaultDbName = usedDefaultDbName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NeoJ4 DB: ");
        sb.append(getLocation());
        return sb.toString();
    }

    public String getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(String oauthToken) {
        this.oauthToken = oauthToken;
    }

    @Override
    public int hashCode() {
        return Objects.hash(auth, database, location, maxConnectionPoolSize, oauthToken, usedDefaultDbName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConnectorConfig other = (ConnectorConfig) obj;
        return Objects.equals(auth, other.auth) && Objects.equals(database, other.database)
                && Objects.equals(location, other.location) && maxConnectionPoolSize == other.maxConnectionPoolSize
                && Objects.equals(oauthToken, other.oauthToken) && Objects.equals(usedDefaultDbName, other.usedDefaultDbName);
    }

    @Override
    public ConnectorConfig clone() {
        try {
            final ConnectorConfig clone = (ConnectorConfig) super.clone();
            if (auth != null) {
                clone.auth = auth.clone();
            }
            clone.oauthToken = this.oauthToken; // Clone the new field
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    public ConnectorConfig createResolvedConfig(final CredentialsProvider cp) throws IOException {
        final ConnectorConfig cfg = clone();
        final AuthConfig auth = cfg.getAuth();

        // If AuthScheme is already OAuth2 and token is present, use it directly.
        // This prevents re-resolving from CredentialsProvider in contexts like ReaderDialog
        // where the provider might not fully resolve JWTCredentials based on principal.
        if (auth != null && auth.getScheme() == AuthScheme.OAuth2) {
            if (cfg.getOauthToken() != null && !cfg.getOauthToken().isEmpty()) {
                // Token is already set, no need to re-resolve.
                // Ensure the scheme is explicitly OAuth2.
                cfg.getAuth().setScheme(AuthScheme.OAuth2);
            } else {
                // If AuthScheme is OAuth2 but token is not yet set, try to resolve from CredentialsProvider.
                // This path is typically taken when the ConnectorModel first resolves the token.
                final ICredentials c = cp.get(auth.getPrincipal());
                if (c instanceof org.knime.credentials.base.oauth.api.JWTCredential) {
                    org.knime.credentials.base.oauth.api.JWTCredential jwt = (org.knime.credentials.base.oauth.api.JWTCredential) c;
                    cfg.setOauthToken(jwt.getAccessToken());
                    cfg.getAuth().setScheme(AuthScheme.OAuth2);
                } else {
                    // If credential resolution fails, but AuthScheme is OAuth2,
                    // it means the token might be missing or the credential provider
                    // couldn't resolve it. In this case, we don't want to fall back
                    // to basic auth, so we keep the scheme as OAuth2 and let
                    // Neo4jSupport handle the missing token (which will throw an IllegalArgumentException).
                    // This ensures we don't accidentally try basic auth.
                }
            }
        } else if (auth != null && auth.getScheme() == AuthScheme.flowCredentials) {
            final ICredentials c = cp.get(auth.getPrincipal());
            cfg.getAuth().setPrincipal(c.getLogin());
            cfg.getAuth().setCredentials(c.getPassword() != null ? c.getPassword() : "");
        } else if (auth == null) {
            // If no AuthConfig exists, try to resolve a JWTCredential directly
            try {
                final ICredentials c = cp.get(null); // Try to get a default credential
                if (c instanceof org.knime.credentials.base.oauth.api.JWTCredential) {
                    org.knime.credentials.base.oauth.api.JWTCredential jwt = (org.knime.credentials.base.oauth.api.JWTCredential) c;
                    cfg.setOauthToken(jwt.getAccessToken());
                    // Create AuthConfig and set scheme to OAuth2
                    AuthConfig newAuth = new AuthConfig();
                    newAuth.setScheme(AuthScheme.OAuth2);
                    cfg.setAuth(newAuth);
                }
            } catch (Exception e) {
                // Ignore if no default credential or not a JWTCredential
            }
        }
        return cfg;
    }
}
