/**
 *
 */
package se.redfield.knime.neo4jextension.cfg;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorPortDataSerializer {
    private static final String RELATIONSHIP_TYPES_KEY = "relationshipTypes";
    private static final String NODE_LABELS_KEY = "nodeLabels";

    public ConnectorPortData load(final ConfigRO model) throws InvalidSettingsException {
        final ConnectorPortData data = new ConnectorPortData();
        data.setConnectorConfig(new ConnectorConfigSerializer().load(model));

        //load node labels
        if (model.containsKey(NODE_LABELS_KEY)) {//check contains key for backward compatibility
            for (final String label : model.getStringArray(NODE_LABELS_KEY)) {
                data.getNodeLabels().add(label);
            }
        }
        //load relationship types
        if (model.containsKey(RELATIONSHIP_TYPES_KEY)) {
            for (final String type : model.getStringArray(RELATIONSHIP_TYPES_KEY)) {
                data.getRelationshipTypes().add(type);
            }
        }

        return data;
    }
    public void save(final ConnectorPortData data, final ConfigWO model) {
        new ConnectorConfigSerializer().save(data.getConnectorConfig(), model);

        //save node labels
        final List<String> nodeLabels = data.getNodeLabels();
        model.addStringArray(NODE_LABELS_KEY,
                nodeLabels.toArray(new String[nodeLabels.size()]));

        //relationship types
        final List<String> relationshipTypes = data.getRelationshipTypes();
        model.addStringArray(RELATIONSHIP_TYPES_KEY,
                relationshipTypes.toArray(new String[relationshipTypes.size()]));
    }
}
