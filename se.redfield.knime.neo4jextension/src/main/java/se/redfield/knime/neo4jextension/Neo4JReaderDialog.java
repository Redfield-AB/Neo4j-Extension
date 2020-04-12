/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JReaderDialog extends NodeDialogPane {
    private final JTextArea scriptEditor = new JTextArea();

    /**
     * Default constructor.
     */
    public Neo4JReaderDialog() {
        super();

        addTab("Script", createScriptPage());
    }

    @Override
    protected void addFlowVariablesTab() {
        // ignore flow variables tab.
    }
    /**
     * @return script editor page.
     */
    private JPanel createScriptPage() {
        final JPanel p = new JPanel(new BorderLayout());
        p.add(scriptEditor, BorderLayout.CENTER);
        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final Neo4JReaderModel model = buildModel();
        if (model != null) {
            model.saveSettingsTo(settings);
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        final Neo4JReaderModel m = new Neo4JReaderModel();
        try {
            m.validateSettings(settings);
            m.loadValidatedSettingsFrom(settings);
            initFromModel(m);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException("Failed to load configuration from settings", e);
        }
    }
    /**
     * @param model model.
     */
    private void initFromModel(final Neo4JReaderModel model) {
        scriptEditor.setText(model.getScript());
    }
    /**
     * @return model.
     */
    private Neo4JReaderModel buildModel() throws InvalidSettingsException{
        final String script = scriptEditor.getText();
        if (script == null || script.trim().isEmpty()) {
            throw new InvalidSettingsException("Invalid script: " + script);
        }

        final Neo4JReaderModel model = new Neo4JReaderModel();
        model.setScript(script);
        return model;
    }
}
