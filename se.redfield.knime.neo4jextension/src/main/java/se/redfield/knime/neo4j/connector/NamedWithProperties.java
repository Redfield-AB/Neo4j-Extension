/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class NamedWithProperties extends Named implements Cloneable {
    private Set<String> properties = new HashSet<String>();

    public NamedWithProperties() {
        super();
    }
    public NamedWithProperties(final String n) {
        this.name = n;
    }
    public Set<String> getProperties() {
        return properties;
    }
    @Override
    public NamedWithProperties clone() {
        try {
            final NamedWithProperties clone = (NamedWithProperties) super.clone();
            clone.properties = new HashSet<String>(this.properties);
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
