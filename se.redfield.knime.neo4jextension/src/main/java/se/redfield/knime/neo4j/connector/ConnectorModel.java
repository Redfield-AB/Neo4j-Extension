/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import se.redfield.knime.neo4j.db.WithSessionAsyncRunnable;

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
        final List<WithSessionAsyncRunnable<Void>> runs = new ArrayList<>(4);

        final Map<String, NamedWithProperties> nodes = new HashMap<>();
        final Map<String, NamedWithProperties> relationships = new HashMap<>();
        final List<FunctionDesc> functions = new LinkedList<>();

        runs.add(s -> loadNamedWithProperties(s, "call db.labels()", nodes));
        runs.add(s -> loadNodeLabelPropertiess(s, nodes));
        runs.add(s -> loadNamedWithProperties(s, "call db.relationshipTypes()", relationships));
        runs.add(s -> loadRelationshipProperties(s, relationships));
        runs.add(s -> loadFunctions(s, functions));

        final Neo4JSupport support = new Neo4JSupport(data.getConnectorConfig());
        support.runAndWait(runs);

        data.setNodeLabels(new LinkedList<NamedWithProperties>(nodes.values()));
        data.setRelationshipTypes(new LinkedList<NamedWithProperties>(relationships.values()));
        data.setFunctions(functions);

        return new PortObject[]{new ConnectorPortObject(data)};
    }

    private void loadNamedWithProperties(final Session s, final String query,
            final Map<String, NamedWithProperties> map) {
        final List<Record> result = s.readTransaction(tx -> tx.run(query).list());
        for (final Record r : result) {
            final String type = r.get(0).asString();
            synchronized (map) {
                if (!map.containsKey(type)) {
                    map.put(type, new NamedWithProperties(type));
                }
            }
        }
    }
    private void loadNodeLabelPropertiess(final Session s, final Map<String, NamedWithProperties> map) {
        final List<Record> result = s.readTransaction(tx -> tx.run(
                "call db.schema.nodeTypeProperties()").list());
        for (final Record r : result) {
            final String property = r.get("propertyName").asString();
            final List<Object> nodeLabels = r.get("nodeLabels").asList();

            for (final Object obj : nodeLabels) {
                final String type = (String) obj;

                NamedWithProperties n;
                synchronized (map) {
                    n = map.get(type);
                    if (n == null) {
                        n = new NamedWithProperties(type);
                        map.put(type, n);
                    }
                }

                n.getProperties().add(property);
            }
        }
    }

    private void loadRelationshipProperties(final Session s, final Map<String, NamedWithProperties> map) {
        final List<Record> result = s.readTransaction(tx -> tx.run(
                "call db.schema.relTypeProperties()").list());
        for (final Record r : result) {
            final String property = r.get("propertyName").asString();
            String type = r.get("relType").asString();
            if (type.startsWith(":")) {
                type = type.substring(2, type.length() - 1);
            }

            NamedWithProperties n;
            synchronized (map) {
                n = map.get(type);
                if (n == null) {
                    n = new NamedWithProperties(type);
                    map.put(type, n);
                }
            }

            n.getProperties().add(property);
        }
    }
    private void loadFunctions(final Session s, final List<FunctionDesc> functions) {
        final List<Record> result = s.readTransaction(tx -> tx.run(
                "call dbms.functions()").list());
        for (final Record r : result) {
            final FunctionDesc f = new FunctionDesc();
            f.setName(r.get("name").asString());
            f.setSignature(r.get("signature").asString());
            f.setDescription(r.get("description").asString());
            functions.add(f);
        }
    }

    private PortObjectSpec[] configure() {
        return new PortObjectSpec[] {new ConnectorSpec(data)};
    }
}
