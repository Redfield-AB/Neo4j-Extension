/**
 *
 */
package se.redfield.knime.neo4jextension;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

import se.redfield.knime.neo4jextension.cfg.AuthConfig;
import se.redfield.knime.neo4jextension.cfg.ConnectorConfig;
import se.redfield.knime.neo4jextension.cfg.ConnectorConfigSerializer;

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
        super();
        this.config = new ConnectorConfig();
        config.reset();

        final AuthConfig auth = new AuthConfig();
        auth.setCredentials("*******");
        auth.setPrincipal("user");
        auth.setScheme("basic");

        config.setAuth(auth);
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
        //also save labels
        model.addStringArray("nodeLabels");
        model.addStringArray("relationshipTypes");
    }
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        config = new ConnectorConfigSerializer().load(model);
        //not need to load labels. Just saved label will shown on related dialog
    }
    /**
     * @return Neo4J connector.
     */
    public ConnectorConfig getConnector() {
        return config;
    }
}
