/**
 *
 */
package se.redfield.knime.neo4j.reader.cfg;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import se.redfield.knime.neo4j.reader.ColumnInfo;

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
        settings.addString("inputColumn", columnToString(config.getInputColumn()));
    }

    public ReaderConfig read(final NodeSettingsRO settings) throws InvalidSettingsException {
        final ReaderConfig config = new ReaderConfig();

        config.setScript(settings.getString("script"));
        //use JSON
        if (settings.containsKey("useJson")) {
            config.setUseJson(settings.getBoolean("useJson"));
        }
        if (settings.containsKey("inputColumn")) {
            config.setInputColumn(columnFromString(settings.getString("inputColumn")));
        }

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
}
