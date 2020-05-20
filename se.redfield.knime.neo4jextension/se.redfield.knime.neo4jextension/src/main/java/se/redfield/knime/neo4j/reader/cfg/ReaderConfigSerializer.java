/**
 *
 */
package se.redfield.knime.neo4j.reader.cfg;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.NamedWithProperties;
import se.redfield.knime.neo4j.reader.ColumnInfo;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderConfigSerializer {
    private static final String STOP_ON_QUERY_FAILURE_KEY = "stopOnQueryFailure";
    private static final String INPUT_COLUMN_KEY = "inputColumn";
    private static final String USE_JSON_KEY = "useJson";
    private static final String SCRIPT_KEY = "script";
    private static final String DESCRIPTION_KEY = "description";
    private static final String SIGNATURE_KEY = "signature";
    private static final String RELATIONSHIP_TYPES_KEY = "relationships";
    private static final String NODE_LABELS_KEY = "labels";
    private static final String FUNCTIONS_KEY = "functions";

    /**
     * Default constructor.
     */
    public ReaderConfigSerializer() {
        super();
    }

    public void write(final ReaderConfig config, final NodeSettingsWO settings) {
        settings.addString(SCRIPT_KEY, config.getScript());
        settings.addBoolean(USE_JSON_KEY, config.isUseJson());
        settings.addString(INPUT_COLUMN_KEY, columnToString(config.getInputColumn()));
        settings.addBoolean(STOP_ON_QUERY_FAILURE_KEY, config.isStopOnQueryFailure());

        saveNamedWithProperties(settings, NODE_LABELS_KEY, config.getNodeLabels());
        saveNamedWithProperties(settings, RELATIONSHIP_TYPES_KEY, config.getRelationshipTypes());
        saveFunctions(settings, config.getFunctions());
    }

    public ReaderConfig read(final NodeSettingsRO settings) throws InvalidSettingsException {
        final ReaderConfig config = new ReaderConfig();

        config.setScript(settings.getString(SCRIPT_KEY));
        //use JSON
        if (settings.containsKey(USE_JSON_KEY)) {
            config.setUseJson(settings.getBoolean(USE_JSON_KEY));
        }
        if (settings.containsKey(INPUT_COLUMN_KEY)) {
            config.setInputColumn(columnFromString(settings.getString(INPUT_COLUMN_KEY)));
        }
        if (settings.containsKey(STOP_ON_QUERY_FAILURE_KEY)) {
            config.setStopOnQueryFailure(settings.getBoolean(STOP_ON_QUERY_FAILURE_KEY));
        }

        config.setNodeLabels(loadNamedWithPropertiesSafely(settings, NODE_LABELS_KEY));
        config.setRelationshipTypes(loadNamedWithPropertiesSafely(settings, RELATIONSHIP_TYPES_KEY));
        config.setFunctions(loadFunctions(settings));

        return config;
    }

    private ColumnInfo columnFromString(final String str) {
        if (str == null) {
            return null;
        }

        final int offset = str.indexOf(':');
        return new ColumnInfo(
                str.substring(offset + 1),
                Integer.parseInt(str.substring(0, offset)));
    }
    private String columnToString(final ColumnInfo col) {
        if (col == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(col.getOffset()).append(':').append(col.getName());
        return sb.toString();
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
                f.setSignature(cfg.getString(SIGNATURE_KEY));
                f.setDescription(cfg.getString(DESCRIPTION_KEY));
            }
        }
        return functions;
    }
    private void saveFunctions(final ConfigWO model, final List<FunctionDesc> functions) {
        final Config root = model.addConfig(FUNCTIONS_KEY);
        for (final FunctionDesc f : functions) {
            final Config cfg = root.addConfig(f.getName());
            cfg.addString(SIGNATURE_KEY, f.getSignature());
            cfg.addString(DESCRIPTION_KEY, f.getDescription());
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
