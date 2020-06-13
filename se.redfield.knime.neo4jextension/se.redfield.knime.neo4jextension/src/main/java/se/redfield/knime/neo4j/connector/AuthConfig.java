/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.util.Objects;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AuthConfig implements Cloneable {
    private AuthScheme scheme;
    private String principal;
    private String credentials;

    public AuthConfig() {
        super();
    }

    public AuthScheme getScheme() {
        return scheme;
    }
    public void setScheme(final AuthScheme scheme) {
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
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AuthConfig)) {
            return false;
        }

        final AuthConfig that = (AuthConfig) obj;
        return Objects.equals(scheme, that.scheme)
                && Objects.equals(principal, that.principal)
                && Objects.equals(credentials, that.credentials);
    }
    @Override
    public int hashCode() {
        return Objects.hash(
                scheme,
                principal,
                credentials);
    }
    @Override
    public AuthConfig clone() {
        try {
            return (AuthConfig) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
