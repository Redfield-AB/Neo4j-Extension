/**
 *
 */
package se.redfield.knime.neo4jextension.cfg;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import se.redfield.knime.neo4jextension.SourceType;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderConfigSerializer {
    /**
     * Default constructor.
     */
    public ReaderConfigSerializer() {
        super();
    }

    public void write(final ReaderConfig config, final NodeSettingsWO settings) {
        settings.addString("script", config.getScript());
        settings.addBoolean("useJson", config.isUseJson());

        settings.addString("source", config.getSource().toString());
        settings.addStringArray("nodeLabels", getStringArray(config.getNodeLabels()));
        settings.addStringArray("relationshipTypes", getStringArray(config.getRelationshipTypes()));
    }
    private String[] getStringArray(final List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    public ReaderConfig read(final NodeSettingsRO settings) throws InvalidSettingsException {
        final ReaderConfig config = new ReaderConfig();

        //script
        if (!settings.containsKey("script")) {
            throw new InvalidSettingsException("Not script found");
        }
        config.setScript(settings.getString("script"));

        //use JSON
        if (settings.containsKey("useJson")) {
            config.setUseJson(settings.getBoolean("useJson"));
        }

        if (settings.containsKey("source")) {
            config.setSource(SourceType.valueOf(settings.getString("source")));
        }
        config.setNodeLabels(getStringsSafely(settings, "nodeLabels"));
        config.setRelationshipTypes(getStringsSafely(settings, "relationshipTypes"));
        return config;
    }
    private List<String> getStringsSafely(final NodeSettingsRO settings, final String key)
            throws InvalidSettingsException {
        final List<String> list = new LinkedList<>();
        if (settings.containsKey(key)) {
            final String[] array = settings.getStringArray(key);
            for (final String str : array) {
                list.add(str);
            }
        }
        return list;
    }
}
