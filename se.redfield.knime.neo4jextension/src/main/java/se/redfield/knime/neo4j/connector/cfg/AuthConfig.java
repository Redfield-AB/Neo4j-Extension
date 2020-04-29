/**
 *
 */
package se.redfield.knime.neo4j.connector.cfg;

import java.util.Objects;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AuthConfig {
    private String scheme;
    private String principal;
    private String credentials;
    private String realm;
    private String parameters;

    public AuthConfig() {
        super();
    }

    public String getScheme() {
        return scheme;
    }
    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }
    public String getPrincipal() {
        return principal;
    }
    public void setPrincipal(final String principal) {
        this.principal = principal;
    }
    public String getCredentials() {
        return credentials;
    }
    public void setCredentials(final String credentials) {
        this.credentials = credentials;
    }
    public String getRealm() {
        return realm;
    }
    public void setRealm(final String realm) {
        this.realm = realm;
    }
    public String getParameters() {
        return parameters;
    }
    public void setParameters(final String parameters) {
        this.parameters = parameters;
    }
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AuthConfig)) {
            return false;
        }

        AuthConfig that = (AuthConfig) obj;
        return Objects.equals(scheme, that.scheme)
                && Objects.equals(principal, that.principal)
                && Objects.equals(credentials, that.credentials)
                && Objects.equals(realm, that.realm)
                && Objects.equals(parameters, that.parameters);
    }
    @Override
    public int hashCode() {
        return Objects.hash(
                scheme,
                principal,
                credentials,
                realm,
                parameters);
    }
}
