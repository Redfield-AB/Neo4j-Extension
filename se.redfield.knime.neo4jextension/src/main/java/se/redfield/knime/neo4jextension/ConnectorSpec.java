/**
 *
 */
package se.redfield.knime.neo4jextension;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

import se.redfield.knime.neo4jextension.cfg.ConnectorConfigSerializer;
import se.redfield.knime.neo4jextension.cfg.ConnectorConfig;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorSpec extends AbstractSimplePortObjectSpec {
    private ConnectorConfig config;

    /**
     * Default constructor.
     */
    public ConnectorSpec() {
        this(new ConnectorConfig());
    }
    /**
     * @param con connector object.
     */
    public ConnectorSpec(final ConnectorConfig con) {
        super();
        this.config = con;
    }
    @Override
    protected void save(final ModelContentWO model) {
        new ConnectorConfigSerializer().save(config, model);
    }
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        config = new ConnectorConfigSerializer().load(model);
    }
    /**
     * @return Neo4J connector.
     */
    public ConnectorConfig getConnector() {
        return config;
    }
}
