/**
 *
 */
package se.redfield.knime.neo4j.connector;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorSpec extends AbstractSimplePortObjectSpec {
    private ConnectorConfig data;

    /**
     * Default constructor.
     */
    public ConnectorSpec() {
        super();
        this.data = new ConnectorConfig();

        final AuthConfig auth = new AuthConfig();
        auth.setCredentials("*******");
        auth.setPrincipal("user");
        auth.setScheme(AuthScheme.basic);

        data.setAuth(auth);
    }
    /**
     * @param con connector object.
     */
    public ConnectorSpec(final ConnectorConfig con) {
        super();
        this.data = con;
    }
    @Override
    protected void save(final ModelContentWO model) {
        new ConnectorConfigSerializer().save(data, model);
    }
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        data = new ConnectorConfigSerializer().load(model);
    }
    /**
     * @return Neo4j connector.
     */
    public ConnectorConfig getPortData() {
        return data;
    }
}
