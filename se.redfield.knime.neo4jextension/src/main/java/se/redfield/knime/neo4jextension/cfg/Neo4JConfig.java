/**
 *
 */
package se.redfield.knime.neo4jextension.cfg;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JConfig {
    private URI location;
    private AuthConfig auth;
    private AdvancedSettings advancedSettings;

    public Neo4JConfig() {
        super();
    }

    public void reset() {
        this.advancedSettings = new AdvancedSettings();

        this.auth = null;
        try {
            this.location = new URI("bolt://localhost:7687");
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
    public AdvancedSettings getAdvancedSettings() {
        return advancedSettings;
    }
    public void setAdvancedSettings(final AdvancedSettings config) {
        this.advancedSettings = config;
    }
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Neo4JConfig)) {
            return false;
        }

        final Neo4JConfig that = (Neo4JConfig) obj;
        return Objects.equals(this.location, that.location)
            && Objects.equals(this.auth, that.auth)
            && Objects.equals(this.advancedSettings, that.advancedSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.location,
                this.auth,
                advancedSettings);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NeoJ4 DB: ");
        sb.append(getLocation());
        return sb.toString();
    }
}
