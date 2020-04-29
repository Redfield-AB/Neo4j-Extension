/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import se.redfield.knime.neo4j.connector.cfg.ConnectorConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorPortDataSerializer {
    private static final String PROPERTY_KEYS_KEY = "propertyKeys";
    private static final String RELATIONSHIP_TYPES_KEY = "relationshipTypes";
    private static final String NODE_LABELS_KEY = "nodeLabels";

    public ConnectorPortData load(final ConfigRO model) throws InvalidSettingsException {
        final ConnectorPortData data = new ConnectorPortData();
        data.setConnectorConfig(new ConnectorConfigSerializer().load(model));

        data.setNodeLabels(loadStringsSafely(model, NODE_LABELS_KEY));
        data.setRelationshipTypes(loadStringsSafely(model, RELATIONSHIP_TYPES_KEY));
        data.setPropertyKeys(loadStringsSafely(model, PROPERTY_KEYS_KEY));

        return data;
    }
    private List<String> loadStringsSafely(final ConfigRO model, final String key)
            throws InvalidSettingsException {
        final List<String> list = new LinkedList<String>();
        if (model.containsKey(key)) {//check contains key for backward compatibility
            for (final String label : model.getStringArray(key)) {
                list.add(label);
            }
        }
        return list;
    }
    public void save(final ConnectorPortData data, final ConfigWO model) {
        new ConnectorConfigSerializer().save(data.getConnectorConfig(), model);

        //property keys
        final List<String> propertyKeys = data.getPropertyKeys();
        model.addStringArray(PROPERTY_KEYS_KEY,
                propertyKeys.toArray(new String[propertyKeys.size()]));

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
