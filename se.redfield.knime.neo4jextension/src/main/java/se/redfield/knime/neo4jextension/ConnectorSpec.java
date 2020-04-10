/**
 *
 */
package se.redfield.knime.neo4jextension;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorSpec extends AbstractSimplePortObjectSpec {
    private final Neo4JConnector connector;

    /**
     * Default constructor.
     */
    public ConnectorSpec() {
        this(new Neo4JConnector());
    }
    /**
     * @param con connector object.
     */
    public ConnectorSpec(final Neo4JConnector con) {
        super();
        this.connector = con;
    }
    @Override
    protected void save(final ModelContentWO model) {
        connector.save(model);
    }
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        connector.load(model);
    }
    /**
     * @return Neo4J connector.
     */
    public Neo4JConnector getConnector() {
        return connector;
    }
}
