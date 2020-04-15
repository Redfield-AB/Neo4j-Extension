/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import se.redfield.knime.neo4jextension.cfg.ReaderConfig;
import se.redfield.knime.neo4jextension.cfg.ReaderConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JReaderDialog extends NodeDialogPane {
    private final JCheckBox useJsonOutput = new JCheckBox();
    private final JTextArea scriptEditor = new JTextArea();

    /**
     * Default constructor.
     */
    public Neo4JReaderDialog() {
        super();

        addTab("Script", createScriptPage());
    }

    /**
     * @return script editor page.
     */
    private JPanel createScriptPage() {
        final JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(new EmptyBorder(5, 5, 5, 5));

        //use JSON
        final JPanel north = new JPanel(new BorderLayout(5, 5));
        north.add(new JLabel("Use JSON output"), BorderLayout.WEST);
        north.add(useJsonOutput, BorderLayout.CENTER);

        p.add(north, BorderLayout.NORTH);

        //Script editor
        p.add(new JScrollPane(scriptEditor), BorderLayout.CENTER);
        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final ReaderConfig model = buildConfig();
        new ReaderConfigSerializer().write(model, settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            initFromModel(new ReaderConfigSerializer().read(settings));
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException("Failed to load configuration from settings", e);
        }
    }
    /**
     * @param model model.
     */
    private void initFromModel(final ReaderConfig model) {
        scriptEditor.setText(model.getScript());
        useJsonOutput.setSelected(model.isUseJson());
    }
    /**
     * @return model.
     */
    private ReaderConfig buildConfig() throws InvalidSettingsException {
        final String script = scriptEditor.getText();
        if (script == null || script.trim().isEmpty()) {
            throw new InvalidSettingsException("Invalid script: " + script);
        }

        final ReaderConfig model = new ReaderConfig();
        model.setScript(script);
        model.setUseJson(this.useJsonOutput.isSelected());
        return model;
    }
}
