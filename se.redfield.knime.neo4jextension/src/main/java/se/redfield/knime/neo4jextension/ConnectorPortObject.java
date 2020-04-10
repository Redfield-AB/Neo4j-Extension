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
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

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
        StringBuilder sb = new StringBuilder("NeoJ4 DB: ");
        sb.append(connector.getConnector().getLocation());
        return sb.toString();
    }
    @Override
    public PortObjectSpec getSpec() {
        return connector;
    }
    public void testConnection() {
        Session s = getSession(connector.getConnector());
        //just tests the session is opened.
        testIsOpen(s);
    }
    public List<Record> run(final String query) {
        URI location = connector.getConnector().getLocation();
        Config config = connector.getConnector().getConfig();
        AuthConfig auth = connector.getConnector().getAuth();

        Driver driver = GraphDatabase.driver(location);
        Session s = driver.session();
        try {
            return s.readTransaction( tx -> tx.run(query).list());
        } finally {
            s.close();
        }
    }
    /**
     * @param s
     */
    private void testIsOpen(final Session s) {
        //TODO uncomment.
        //s.isOpen();
    }
    private Session getSession(final Neo4JConnector c) {
        //TODO get sesson from session pool.
        return null;
    }
}
