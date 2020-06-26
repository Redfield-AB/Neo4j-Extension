/**
 *
 */
package se.redfield.knime.neo4j.connector;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Named {
    protected String name;

    public Named() {
        super();
    }
    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }
}
