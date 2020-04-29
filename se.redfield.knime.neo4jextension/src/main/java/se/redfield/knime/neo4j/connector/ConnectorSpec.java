/**
 *
 */
package se.redfield.knime.neo4j.connector;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

import se.redfield.knime.neo4j.connector.cfg.AuthConfig;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorSpec extends AbstractSimplePortObjectSpec {
    private ConnectorPortData data;

    /**
     * Default constructor.
     */
    public ConnectorSpec() {
        super();
        this.data = new ConnectorPortData();

        final AuthConfig auth = new AuthConfig();
        auth.setCredentials("*******");
        auth.setPrincipal("user");
        auth.setScheme("basic");

        data.getConnectorConfig().setAuth(auth);
    }
    /**
     * @param con connector object.
     */
    public ConnectorSpec(final ConnectorPortData con) {
        super();
        this.data = con;
    }
    @Override
    protected void save(final ModelContentWO model) {
        new ConnectorPortDataSerializer().save(data, model);
    }
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        data = new ConnectorPortDataSerializer().load(model);
    }
    /**
     * @return Neo4J connector.
     */
    public ConnectorPortData getPortData() {
        return data;
    }
}
