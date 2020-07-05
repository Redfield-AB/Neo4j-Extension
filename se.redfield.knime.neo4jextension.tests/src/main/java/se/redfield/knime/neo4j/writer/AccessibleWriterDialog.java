/**
 *
 */
package se.redfield.knime.neo4j.writer;

import javax.swing.ImageIcon;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class AccessibleWriterDialog extends WriterDialog {
    /**
     * Default constructor.
     */
    public AccessibleWriterDialog() {
        super();
    }
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
    }
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
    }
    @Override
    protected ImageIcon createRefreshIcon() {
        return null;
    }
}
