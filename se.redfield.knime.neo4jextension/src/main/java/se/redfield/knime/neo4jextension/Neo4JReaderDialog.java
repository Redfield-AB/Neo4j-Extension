/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;

import se.redfield.knime.neo4jextension.cfg.ReaderConfig;
import se.redfield.knime.neo4jextension.cfg.ReaderConfigSerializer;
import se.redfield.knime.neo4jextension.ui.StringSelectionPane;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JReaderDialog extends DataAwareNodeDialogPane {
    private final JCheckBox useJsonOutput = new JCheckBox();
    private final JTextArea scriptEditor = new JTextArea();
    private JComboBox<SourceType> selectInput = new JComboBox<>();

    private JScrollPane scriptEditorPane;
    private JPanel sourcesContainer;
    private JTabbedPane nodeOrRelationshipSelectionPane;
    private StringSelectionPane nodeSelectPane;
    private StringSelectionPane relationshiptSelectPane;

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
        final JPanel useJsonOutputPane = new JPanel(new BorderLayout(5, 5));
        useJsonOutputPane.add(new JLabel("Use JSON output"), BorderLayout.WEST);
        useJsonOutputPane.add(useJsonOutput, BorderLayout.CENTER);

        //select source
        selectInput.addItem(SourceType.Script);
        selectInput.addItem(SourceType.Labels);
        selectInput.setSelectedItem(SourceType.Script);
        selectInput.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                final String text = value == SourceType.Script ? "Script" : "Item classes";
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        final JPanel selectInputPane = new JPanel(new BorderLayout(5, 5));
        selectInputPane.add(new JLabel("Select data input:"), BorderLayout.WEST);
        selectInputPane.add(selectInput, BorderLayout.CENTER);
        selectInput.addActionListener(e -> showSource((SourceType) selectInput.getSelectedItem()));

        final JPanel north = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        north.add(useJsonOutputPane);
        north.add(selectInputPane);

        p.add(north, BorderLayout.NORTH);

        //Script editor
        scriptEditorPane = new JScrollPane(scriptEditor);

        //Node label selection
        nodeOrRelationshipSelectionPane = new JTabbedPane(JTabbedPane.TOP);

        nodeSelectPane = new StringSelectionPane();
        relationshiptSelectPane = new StringSelectionPane();
        nodeOrRelationshipSelectionPane.addTab("Nodes", nodeSelectPane);
        nodeOrRelationshipSelectionPane.addTab("Relationships", relationshiptSelectPane);

        sourcesContainer = new JPanel(new BorderLayout());
        p.add(sourcesContainer, BorderLayout.CENTER);

        showSource(SourceType.Script);
        return p;
    }

    /**
     * @param selection
     */
    private void showSource(final SourceType selection) {
        JComponent toAdd;
        if (selection == SourceType.Script) {
            toAdd = scriptEditorPane;
        } else {
            toAdd = nodeOrRelationshipSelectionPane;
        }

        sourcesContainer.removeAll();
        sourcesContainer.add(toAdd, BorderLayout.CENTER);
        sourcesContainer.revalidate();
        sourcesContainer.repaint();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final ReaderConfig model = buildConfig();
        new ReaderConfigSerializer().write(model, settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input) throws NotConfigurableException {
        try {
            final ReaderConfig model = new ReaderConfigSerializer().read(settings);
            initFromModel(model, (ConnectorPortObject) input[0]);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException("Failed to load configuration from settings", e);
        }
    }

    /**
     * @param model model.
     */
    private void initFromModel(final ReaderConfig model, final ConnectorPortObject connector) {
        scriptEditor.setText(model.getScript());
        useJsonOutput.setSelected(model.isUseJson());

        initStringSelectionPane(nodeSelectPane,
                connector.getNodeLabels(), model.getNodeLabels());
        initStringSelectionPane(relationshiptSelectPane,
                connector.getRelationshipTypes(), model.getRelationshipTypes());
        showSource(model.getSource());
    }
    /**
     * @param panel
     * @param allOrigin
     * @param selectedOrigin
     */
    private void initStringSelectionPane(final StringSelectionPane panel, final List<String> allOrigin,
            final List<String> selectedOrigin) {
        final Set<String> allSet = new HashSet<>(allOrigin);
        final Set<String> selectedSet = new HashSet<>(selectedOrigin);

        final Iterator<String> iter = selectedSet.iterator();
        while (iter.hasNext()) {
            final String next = iter.next();
            if (!allSet.contains(next)) {
                iter.remove();
            } else {
                allSet.remove(next);
            }
        }

        panel.init(new LinkedList<String>(allSet), new LinkedList<String>(selectedSet));
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

        model.setNodeLabels(this.nodeSelectPane.getSelection());
        model.setRelationshipTypes(relationshiptSelectPane.getSelection());
        model.setSource((SourceType) this.selectInput.getSelectedItem());
        return model;
    }
}
