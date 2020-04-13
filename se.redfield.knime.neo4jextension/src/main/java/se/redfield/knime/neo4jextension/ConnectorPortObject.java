/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.net.URI;
import java.util.List;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.TypeSystem;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorPortObject extends AbstractSimplePortObject {
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(
            ConnectorPortObject.class);
    public static final PortType TYPE_OPTIONAL = PortTypeRegistry.getInstance().getPortType(
            ConnectorPortObject.class, true);

    private final ConnectorSpec connector;
    private Driver driver;

    public ConnectorPortObject() {
        this(new ConnectorSpec());
    }
    public ConnectorPortObject(final ConnectorSpec connector) {
        super();
        this.connector = connector;
    }

    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
            throws InvalidSettingsException, CanceledExecutionException {
        this.connector.load(model);
    }
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        this.connector.save(model);
    }
    @Override
    public String getSummary() {
        final StringBuilder sb = new StringBuilder("NeoJ4 DB: ");
        sb.append(connector.getConnector().getLocation());
        return sb.toString();
    }
    @Override
    public ConnectorSpec getSpec() {
        return connector;
    }
    public List<Record> run(final String query) {
        final Driver driver = getDriver();
        final Session s = driver.session();
        try {
            return s.readTransaction( tx -> tx.run(query).list());
        } finally {
            s.close();
        }
    }
    /**
     * @return
     */
    private Driver getDriver() {
        if (this.driver == null) {
            this.driver = createDriver(connector.getConnector());
        }
        return driver;
    }
    /**
     * @param con
     * @return
     */
    static Driver createDriver(final Neo4JConnector con) {
        final URI location = con.getLocation();
        //final Config config = connector.getConnector().getConfig();
        final AuthConfig auth = con.getAuth();

        Driver d;
        if (auth == null) {
            d = GraphDatabase.driver(location);
        } else {
            final AuthToken token = AuthTokens.basic(
                    auth.getPrincipal(), auth.getCredentials(), null);
            d = GraphDatabase.driver(location, token);
        }
        return d;
    }
    public TypeSystem getTypeSystem() {
        return getDriver().defaultTypeSystem();
    }
}
