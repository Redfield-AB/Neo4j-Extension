/**
 *
 */
package se.redfield.knime.neo4j.writer;

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

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class WriterConfigSerializer {
    private static final String KEEP_SOURCE_ROWS_ORDER = "keepSourceRowsOrder";
    private static final String STOP_ON_QUERY_FAILURE_KEY = "stopOnQueryFailure";
    private static final String INPUT_COLUMN_KEY = "inputColumn";
    private static final String USE_ASYNC_KEY = "useAsyncExecution";
    private static final String SCRIPT_KEY = "script";
    private static final String DESCRIPTION_KEY = "description";
    private static final String SIGNATURE_KEY = "signature";
    private static final String RELATIONSHIP_TYPES_KEY = "relationships";
    private static final String NODE_LABELS_KEY = "labels";
    private static final String FUNCTIONS_KEY = "functions";
    private static final String USE_BATCH_KEY = "useBatch";
    private static final String BATCH_KEY = "batchScript";
    private static final String BATCH_PARAMETER_NAME_KEY = "batchParameterName";

    /**
     * Default constructor.
     */
    public WriterConfigSerializer() {
        super();
    }

    public void save(final WriterConfig config, final NodeSettingsWO settings) {
        settings.addString(SCRIPT_KEY, config.getScript());
        settings.addBoolean(USE_ASYNC_KEY, config.isUseAsync());
        settings.addString(INPUT_COLUMN_KEY, config.getInputColumn());
        settings.addBoolean(STOP_ON_QUERY_FAILURE_KEY, config.isStopOnQueryFailure());
        settings.addBoolean(KEEP_SOURCE_ROWS_ORDER, config.isKeepSourceOrder());
        settings.addBoolean(USE_BATCH_KEY, config.isUseBatch());
        settings.addString(BATCH_KEY, config.getBatchScript());
        settings.addString(BATCH_PARAMETER_NAME_KEY, config.getBatchParameterName());

        saveNamedWithProperties(settings, NODE_LABELS_KEY, config.getNodeLabels());
        saveNamedWithProperties(settings, RELATIONSHIP_TYPES_KEY, config.getRelationshipTypes());
        saveFunctions(settings, config.getFunctions());
    }

    public WriterConfig read(final NodeSettingsRO settings) throws InvalidSettingsException {
        final WriterConfig config = new WriterConfig();

        config.setScript(settings.getString(SCRIPT_KEY));
        //use JSON
        if (settings.containsKey(USE_ASYNC_KEY)) {
            config.setUseAsync(settings.getBoolean(USE_ASYNC_KEY));
        }
        if (settings.containsKey(INPUT_COLUMN_KEY)) {
            config.setInputColumn(settings.getString(INPUT_COLUMN_KEY));
        }
        if (settings.containsKey(STOP_ON_QUERY_FAILURE_KEY)) {
            config.setStopOnQueryFailure(settings.getBoolean(STOP_ON_QUERY_FAILURE_KEY));
        }
        if (settings.containsKey(KEEP_SOURCE_ROWS_ORDER)) {
            config.setKeepSourceOrder(settings.getBoolean(KEEP_SOURCE_ROWS_ORDER));
        }
        if (settings.containsKey(BATCH_KEY)) {
            config.setBatchScript(settings.getString(BATCH_KEY));
        }
        if (settings.containsKey(USE_BATCH_KEY)) {
            config.setUseBatch(settings.getBoolean(USE_BATCH_KEY));
        }
        if (settings.containsKey(BATCH_PARAMETER_NAME_KEY)) {
            config.setBatchParameterName(settings.getString(BATCH_PARAMETER_NAME_KEY));
        }

        config.setNodeLabels(loadNamedWithPropertiesSafely(settings, NODE_LABELS_KEY));
        config.setRelationshipTypes(loadNamedWithPropertiesSafely(settings, RELATIONSHIP_TYPES_KEY));
        config.setFunctions(loadFunctions(settings));

        return config;
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
