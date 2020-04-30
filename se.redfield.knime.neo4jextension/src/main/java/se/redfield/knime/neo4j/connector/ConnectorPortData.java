/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.util.LinkedList;
import java.util.List;

import se.redfield.knime.neo4j.connector.cfg.ConnectorConfig;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorPortData {
    private ConnectorConfig config;

    private List<NamedWithProperties> nodeLabels = new LinkedList<>();
    private List<NamedWithProperties> relationshipTypes = new LinkedList<>();
    private List<FunctionDesc> functions = new LinkedList<>();

    public ConnectorPortData() {
        config = new ConnectorConfig();
    }

    public void setConnectorConfig(final ConnectorConfig cfg) {
        this.config = cfg;
    }
    public ConnectorConfig getConnectorConfig() {
        return config;
    }
    public List<NamedWithProperties> getNodeLabels() {
        return nodeLabels;
    }
    public List<NamedWithProperties> getRelationshipTypes() {
        return relationshipTypes;
    }
    public void setNodeLabels(final List<NamedWithProperties> nodeLabels) {
        this.nodeLabels = nodeLabels;
    }
    public void setRelationshipTypes(final List<NamedWithProperties> relationshipTypes) {
        this.relationshipTypes = relationshipTypes;
    }
    public List<FunctionDesc> getFunctions() {
        return functions;
    }
    public void setFunctions(final List<FunctionDesc> functions) {
        this.functions = functions;
    }
}
