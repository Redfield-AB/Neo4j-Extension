/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.util.LinkedList;
import java.util.List;

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
    private List<String> nodeLabels = new LinkedList<>();
    private List<String> relationshipTypes = new LinkedList<>();

    /**
     * Default constructor.
     */
    public ConnectorSpec() {
        super();
        this.config = new ConnectorConfig();

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
        model.addStringArray("nodeLabels", toArray(nodeLabels));
        model.addStringArray("relationshipTypes", toArray(relationshipTypes));
    }
    private String[] toArray(final List<String> list) {
        return list.toArray(new String[list.size()]);
    }
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        config = new ConnectorConfigSerializer().load(model);
        if (model.containsKey("nodeLabels")) {
            nodeLabels = fromArray(model.getStringArray("nodeLabels"));
        }
        if (model.containsKey("relationshipTypes")) {
            relationshipTypes = fromArray(model.getStringArray("relationshipTypes"));
        }
    }
    private List<String> fromArray(final String[] array) {
        final List<String> list = new LinkedList<String>();
        for (final String str : array) {
            list.add(str);
        }
        return list;
    }
    /**
     * @param labels node labels.
     */
    public void setNodeLabels(final List<String> labels) {
        this.nodeLabels = labels;
    }
    /**
     * @return node labels.
     */
    public List<String> getNodeLabels() {
        return nodeLabels;
    }
    /**
     * @param labels relationship labels.
     */
    public void setRelationshipTypes(final List<String> labels) {
        this.relationshipTypes = labels;
    }
    /**
     * @return relationship labels.
     */
    public List<String> getRelationshipTypes() {
        return relationshipTypes;
    }
    /**
     * @return Neo4J connector.
     */
    public ConnectorConfig getConnector() {
        return config;
    }
}
