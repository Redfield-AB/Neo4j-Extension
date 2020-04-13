/**
 *
 */
package se.redfield.knime.neo4jextension;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

import se.redfield.knime.neo4jextension.cfg.Neo4JConfig;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorSpec extends AbstractSimplePortObjectSpec {
    private Neo4JConfig config;

    /**
     * Default constructor.
     */
    public ConnectorSpec() {
        this(new Neo4JConfig());
    }
    /**
     * @param con connector object.
     */
    public ConnectorSpec(final Neo4JConfig con) {
        super();
        this.config = con;
    }
    @Override
    protected void save(final ModelContentWO model) {
        new ConfigSerializer().save(config, model);
    }
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        config = new ConfigSerializer().load(model);
    }
    /**
     * @return Neo4J connector.
     */
    public Neo4JConfig getConnector() {
        return config;
    }
}
