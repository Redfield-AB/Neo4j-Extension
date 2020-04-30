/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

import se.redfield.knime.neo4j.db.Neo4JSupport;
import se.redfield.knime.neo4j.db.WithSessionRunnable;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorModel extends NodeModel {
    private ConnectorPortData data;

    /**
     * Default constructor.
     */
    public ConnectorModel() {
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
        final List<Exception> errors = new LinkedList<Exception>();

        final List<WithSessionRunnable<Void>> runs = new ArrayList<>(3);
        runs.add(s -> {
            try {
                addNodeLabels(s);
            } catch (final Exception e) {
                errors.add(e);
            }
            return null;
        });
        runs.add(s -> {
            try {
                addRelationshipLabels(s);
            } catch (final Exception e) {
                errors.add(e);
            }
            return null;
        });

        final Neo4JSupport support = new Neo4JSupport(data.getConnectorConfig());
        support.runAndWait(runs);

        if (!errors.isEmpty()) {
            final Exception exc = errors.remove(0);
            for (final Exception e : errors) {
                exc.addSuppressed(e);
            }

            throw exc;
        }

        return new PortObject[]{new ConnectorPortObject(data)};
    }
    /**
     * @param s session.
     * @return
     */
    private void addRelationshipLabels(final Session s) {
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
    }
    /**
     * @param s session.
     * @return
     */
    private void addNodeLabels(final Session s) {
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
    }
    private PortObjectSpec[] configure() {
        return new PortObjectSpec[] {new ConnectorSpec(data)};
    }
}
