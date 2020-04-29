/**
 *
 */
package se.redfield.knime.neo4j.reader.cfg;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

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

        return config;
    }
}
