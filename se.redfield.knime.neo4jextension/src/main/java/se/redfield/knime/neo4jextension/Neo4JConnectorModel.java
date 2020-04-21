/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import se.redfield.knime.neo4jextension.cfg.ConnectorConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JConnectorModel extends NodeModel {
    private ConnectorPortObject connector;

    /**
     * Default constructor.
     */
    public Neo4JConnectorModel() {
        super(new PortType[0], new PortType[] {ConnectorPortObject.TYPE});
        this.connector = new ConnectorPortObject();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        connector.save(settings);
    }
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        connector.load(settings);
    }
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //attempt to load settings.
        new ConnectorConfigSerializer().load(settings);
    }
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {}
    @Override
    protected void reset() {
    }
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return configure();
    }
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[0];
    }
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        //load node classes
        connector.<Void>runWithSession(s -> addNodeLabels(s));
        connector.<Void>runWithSession(s -> addRelationshipLabels(s));

        return new PortObject[]{connector};
    }

    /**
     * @param s session.
     * @return
     */
    private Void addRelationshipLabels(final Session s) {
        final StringBuilder query = new StringBuilder("MATCH (n)\n");
        query.append("WITH DISTINCT labels(n) AS labels\n");
        query.append("UNWIND labels AS label\n");
        query.append("RETURN DISTINCT label\n");
        query.append("ORDER BY label\n");

        final List<String> labels = new LinkedList<>();
        final List<Record> result = s.readTransaction(tx -> tx.run(query.toString()).list());

        for (final Record r : result) {
            labels.add(r.get(0).asString());
        }

        connector.setNodeLabels(labels);
        return null;
    }

    /**
     * @param s session.
     * @return
     */
    private Void addNodeLabels(final Session s) {
        final StringBuilder query = new StringBuilder("MATCH ()-[r]-()\n");
        query.append("WITH DISTINCT TYPE(r) AS labels\n");
        query.append("UNWIND labels AS label\n");
        query.append("RETURN DISTINCT label\n");
        query.append("ORDER BY label\n");

        final List<Record> result = s.readTransaction(tx -> tx.run(query.toString()).list());

        final List<String> labels = new LinkedList<>();
        for (final Record r : result) {
            labels.add(r.get(0).asString());
        }

        connector.setRelationshipTypes(labels);
        return null;
    }

    private PortObjectSpec[] configure() {
        return new PortObjectSpec[] {createSpec(connector)};
    }
    private ConnectorSpec createSpec(final ConnectorPortObject c) {
        final ConnectorSpec s = new ConnectorSpec(c.getConnector());
        s.setNodeLabels(c.getNodeLabels());
        s.setRelationshipTypes(c.getRelationshipTypes());
        return s;
    }
}
