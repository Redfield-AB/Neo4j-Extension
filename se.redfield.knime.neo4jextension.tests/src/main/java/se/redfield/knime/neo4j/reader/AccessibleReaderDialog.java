/**
 *
 */
package se.redfield.knime.neo4j.reader;

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
class AccessibleReaderDialog extends ReaderDialog {
    /**
     * Default constructor.
     */
    public AccessibleReaderDialog() {
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
