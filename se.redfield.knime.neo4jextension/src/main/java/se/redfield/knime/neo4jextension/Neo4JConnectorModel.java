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

import se.redfield.knime.neo4jextension.cfg.ConnectorPortData;
import se.redfield.knime.neo4jextension.cfg.ConnectorPortDataSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JConnectorModel extends NodeModel {
    private ConnectorPortData data;

    /**
     * Default constructor.
     */
    public Neo4JConnectorModel() {
        super(new PortType[0], new PortType[] {ConnectorPortObject.TYPE});
        this.data = new ConnectorPortData();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        new ConnectorPortDataSerializer().save(data, settings);
    }
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        data = new ConnectorPortDataSerializer().load(settings);
    }
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //attempt to load settings.
        new ConnectorPortDataSerializer().load(settings);
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
        final ConnectorPortObject portObject = new ConnectorPortObject(data);

        portObject.<Void>runWithSession(s -> addNodeLabels(s));
        portObject.<Void>runWithSession(s -> addRelationshipLabels(s));
        portObject.<Void>runWithSession(s -> addPropertyKeys(s));

        return new PortObject[]{portObject};
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

        data.setNodeLabels(labels);
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

        data.setRelationshipTypes(labels);
        return null;
    }
    private Void addPropertyKeys(final Session s) {
        final StringBuilder query = new StringBuilder("MATCH (n)\n");
        query.append("WITH KEYS(n) AS keys\n");
        query.append("UNWIND keys AS key\n");
        query.append("RETURN DISTINCT key\n");
        query.append("ORDER BY key\n");

        final List<Record> result = s.readTransaction(tx -> tx.run(query.toString()).list());

        final List<String> labels = new LinkedList<>();
        for (final Record r : result) {
            labels.add(r.get(0).asString());
        }

        data.setPropertyKeys(labels);
        return null;
    }

    private PortObjectSpec[] configure() {
        return new PortObjectSpec[] {new ConnectorSpec(data)};
    }
}
