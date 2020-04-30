/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import se.redfield.knime.neo4j.connector.cfg.ConnectorConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorPortDataSerializer {
    private static final String RELATIONSHIP_TYPES_KEY = "relationships";
    private static final String NODE_LABELS_KEY = "labels";
    private static final String FUNCTIONS_KEY = "functions";

    public ConnectorPortData load(final ConfigRO model) throws InvalidSettingsException {
        final ConnectorPortData data = new ConnectorPortData();
        data.setConnectorConfig(new ConnectorConfigSerializer().load(model));

        data.setNodeLabels(loadNamedWithPropertiesSafely(model, NODE_LABELS_KEY));
        data.setRelationshipTypes(loadNamedWithPropertiesSafely(model, RELATIONSHIP_TYPES_KEY));
        data.setFunctions(loadFunctions(model));

        return data;
    }
    private List<NamedWithProperties> loadNamedWithPropertiesSafely(final ConfigRO model, final String key)
            throws InvalidSettingsException {
        final List<NamedWithProperties> list = new LinkedList<>();
        if (model.containsKey(key)) {//check contains key for backward compatibility
            final Config root = model.getConfig(key);
            for (final String name : root) {
                final NamedWithProperties n = new NamedWithProperties(name);
                final String[] props = root.getStringArray(name);
                for (final String p : props) {
                    n.getProperties().add(p);
                }
                list.add(n);
            }
        }
        return list;
    }
    private List<FunctionDesc> loadFunctions(final ConfigRO model) throws InvalidSettingsException {
        final List<FunctionDesc> functions = new LinkedList<>();
        if (model.containsKey(FUNCTIONS_KEY)) {
            final Config root = model.getConfig(FUNCTIONS_KEY);
            for (final String name : root) {
                final FunctionDesc f = new FunctionDesc(name);
                functions.add(f);

                final Config cfg = root.getConfig(name);
                f.setSignature(cfg.getString("signature"));
                f.setDescription(cfg.getString("description"));
            }
        }
        return functions;
    }
    public void save(final ConnectorPortData data, final ConfigWO model) {
        new ConnectorConfigSerializer().save(data.getConnectorConfig(), model);

        saveNamedWithProperties(model, NODE_LABELS_KEY, data.getNodeLabels());
        saveNamedWithProperties(model, RELATIONSHIP_TYPES_KEY, data.getRelationshipTypes());
        saveFunctions(model, data.getFunctions());
    }
    private void saveFunctions(final ConfigWO model, final List<FunctionDesc> functions) {
        final Config root = model.addConfig(FUNCTIONS_KEY);
        for (final FunctionDesc f : functions) {
            final Config cfg = root.addConfig(f.getName());
            cfg.addString("signature", f.getSignature());
            cfg.addString("description", f.getDescription());
        }
    }
    private void saveNamedWithProperties(final ConfigWO model, final String key,
            final List<NamedWithProperties> named) {
        final Config cfg = model.addConfig(key);
        for (final NamedWithProperties n : named) {
            final String[] array = n.getProperties().toArray(new String[n.getProperties().size()]);
            cfg.addStringArray(n.getName(), array);
        }
    }
}
