/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorConfig implements Cloneable {
    private URI location;
    private AuthConfig auth;
    private int maxConnectionPoolSize;
    private String database;

    public ConnectorConfig() {
        super();
        maxConnectionPoolSize = Math.max(Runtime.getRuntime().availableProcessors(), 1);
        try {
            this.location = new URI("bolt://localhost:7687");
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
        auth = new AuthConfig();
        auth.setScheme(AuthScheme.basic);
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NeoJ4 DB: ");
        sb.append(getLocation());
        return sb.toString();
    }
    
    @Override
	public int hashCode() {
		return Objects.hash(auth, database, location, maxConnectionPoolSize);
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
				&& Objects.equals(location, other.location) && maxConnectionPoolSize == other.maxConnectionPoolSize;
	}

	@Override
    public ConnectorConfig clone() {
        try {
            final ConnectorConfig clone = (ConnectorConfig) super.clone();
            if (auth != null) {
                clone.auth = auth.clone();
            }
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
    public ConnectorConfig createResolvedConfig(final CredentialsProvider cp) {
        final ConnectorConfig cfg = clone();
        final AuthConfig auth = cfg.getAuth();
        if (auth != null && auth.getScheme() == AuthScheme.flowCredentials) {
            final ICredentials c = cp.get(auth.getPrincipal());
            cfg.getAuth().setPrincipal(c.getLogin());
            cfg.getAuth().setCredentials(c.getPassword());
        }
        return cfg;
    }
}
