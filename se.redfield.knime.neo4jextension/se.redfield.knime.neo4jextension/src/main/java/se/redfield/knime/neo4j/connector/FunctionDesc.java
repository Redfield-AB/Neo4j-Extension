/**
 *
 */
package se.redfield.knime.neo4j.connector;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class FunctionDesc extends Named {
    private String name;
    private String signature;
    private String description;

    public FunctionDesc() {
        super();
    }
    public FunctionDesc(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }
    public String getSignature() {
        return signature;
    }
    public void setSignature(final String signature) {
        this.signature = signature;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(final String description) {
        this.description = description;
    }
}
