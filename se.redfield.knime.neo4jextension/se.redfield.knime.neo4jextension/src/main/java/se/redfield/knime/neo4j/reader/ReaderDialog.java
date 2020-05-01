/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;

import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

import se.redfield.knime.neo4j.connector.ConnectorPortData;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.NamedWithProperties;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfig;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderDialog extends DataAwareNodeDialogPane {
    private final JCheckBox useJsonOutput = new JCheckBox();
    private JTextArea scriptEditor;

    private JSplitPane sourcesContainer;

    private DefaultListModel<FlowVariable> flowVariables = new DefaultListModel<>();
    private DefaultListModel<NamedWithProperties> nodes = new DefaultListModel<>();
    private DefaultListModel<String> nodeProperties = new DefaultListModel<>();
    private DefaultListModel<NamedWithProperties> relationships = new DefaultListModel<>();
    private DefaultListModel<String> relationshipsProperties = new DefaultListModel<>();
    private DefaultListModel<FunctionDesc> functions = new DefaultListModel<>();

    /**
     * Default constructor.
     */
    public ReaderDialog() {
        super();

        addTab("Script", createScriptPage(), false);
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

        final JPanel north = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        north.add(useJsonOutputPane);

        //Script editor
        final JPanel scriptPanel = new JPanel(new BorderLayout());
        scriptPanel.add(north, BorderLayout.NORTH);
        this.scriptEditor = createScriptEditor();

        final JScrollPane sp = new JScrollPane(scriptEditor,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scriptPanel.add(sp, BorderLayout.CENTER);

        //Labels tree
        final JSplitPane leftSide = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSide.setTopComponent(createFlowVariablesAndLabels());
        leftSide.setBottomComponent(createRelationshipsAndFunctions());

        //Node label selection
        sourcesContainer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        sourcesContainer.setLeftComponent(leftSide);
        sourcesContainer.setRightComponent(scriptPanel);
        p.add(sourcesContainer, BorderLayout.CENTER);

        return p;
    }

    private JSplitPane createFlowVariablesAndLabels() {
        final JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sp.setTopComponent(createFlowVariables());
        sp.setBottomComponent(createNodes());
        return sp;
    }
    private JSplitPane createNodes() {
        return createNamedWithPropertiesComponent(nodes, nodeProperties, "Node labels",
                "Node type properties",
                v -> insertToScript("(:" + v.getName() + ")"));
    }
    private JSplitPane createRelationships() {
        return createNamedWithPropertiesComponent(relationships, relationshipsProperties,
                "Relationship types", "Relationship type properties",
                v -> insertToScript("-[:" + v.getName() + "]-"));
    }

    private JSplitPane createNamedWithPropertiesComponent(final DefaultListModel<NamedWithProperties> named,
            final DefaultListModel<String> propsOfNamed, final String title,
            final String propertiesTitle, final ValueInsertHandler<NamedWithProperties> handler) {
        final JSplitPane p = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        final JPanel nodesContainer = new JPanel(new BorderLayout());
        nodesContainer.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), title));

        final JList<NamedWithProperties> nodesList = createListWithHandler(named, handler);
        nodesContainer.add(nodesList, BorderLayout.CENTER);

        nodesList.setCellRenderer(new NamedWithPropertiesRenderer());
        nodesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                final int index = nodesList.getSelectedIndex();
                if (index > -1) {
                    final NamedWithProperties selected = nodesList.getModel().getElementAt(index);
                    propsOfNamed.clear();
                    for (final String prop : selected.getProperties()) {
                        propsOfNamed.addElement(prop);
                    }
                }
            }
        });

        nodesContainer.add(new JScrollPane(nodesList));
        p.setLeftComponent(nodesContainer);

        final JPanel props = createTitledList(propertiesTitle, propsOfNamed, v -> insertToScript(v));
        p.setRightComponent(props);
        return p;
    }

    private JComponent createFlowVariables() {
        final JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Flow variables"));

        final JList<FlowVariable> list = createListWithHandler(flowVariables,
                v -> insertToScript("${{" + v.getName() + "}}"));
        list.setCellRenderer(new FlowVariableListCellRenderer());
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }
    private JPanel createTitledList(final String title,
            final DefaultListModel<String> listModel, final ValueInsertHandler<String> h) {
        final JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), title));

        final JList<String> list = createListWithHandler(listModel, h);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    private <T> JList<T> createListWithHandler(final DefaultListModel<T> listModel,
            final ValueInsertHandler<T> h) {
        final JList<T> list = new JList<T>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(new ListClickListener<T>(list, h));
        return list;
    }
    private Component createRelationshipsAndFunctions() {
        final JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sp.setTopComponent(createRelationships());
        sp.setBottomComponent(createFunctions());
        return sp;
    }
    private JPanel createFunctions() {
        final JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Functions"));

        final JList<FunctionDesc> list = createListWithHandler(functions, v -> insertToScript(v.getName()));
        list.setCellRenderer(new FunctionDescRenderer());
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    /**
     * @return script editor.
     */
    private JTextArea createScriptEditor() {
        final JTextArea scriptEditor = new JTextArea() {
            private static final long serialVersionUID = -6583141839191451218L;
            @Override
            public Dimension getMinimumSize() {
                //allways return minimum width as zero
                final Dimension min = super.getMinimumSize();
                return min == null ? new Dimension() : new Dimension(0, min.height);
            }
        };

        scriptEditor.setLineWrap(true);
        return scriptEditor;
    }
    private void insertToScript(final String text) {
        //possible remove selection
        final int selStart = scriptEditor.getSelectionStart();
        final int selEnd = scriptEditor.getSelectionEnd();
        if (selEnd != selStart) {
            try {
                scriptEditor.getDocument().remove(
                        Math.min(selStart, selEnd),
                        Math.abs(selStart - selEnd));
            } catch (final BadLocationException e) {
            }
        }

        //insert text
        final int pos = Math.max(0, scriptEditor.getCaretPosition());
        scriptEditor.insert(text, pos);
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
            initFromModel(model, ((ConnectorPortObject) input[1]).getPortData());
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException("Failed to load configuration from settings", e);
        }
    }

    /**
     * @param model model.
     */
    private void initFromModel(final ReaderConfig model, final ConnectorPortData data) {
        scriptEditor.setText(model.getScript());
        useJsonOutput.setSelected(model.isUseJson());

        //clear all lists
        flowVariables.clear();
        nodes.clear();
        nodeProperties.clear();
        relationships.clear();
        relationshipsProperties.clear();
        functions.clear();

        final Map<String, FlowVariable> vars = getAvailableFlowVariables(
                ReaderModel.getFlowVariableTypes());
        for (final FlowVariable var : vars.values()) {
            flowVariables.addElement(var);
        }

        values(nodes, data.getNodeLabels());
        values(relationships, data.getRelationshipTypes());
        values(functions, data.getFunctions());
    }

    private <T> void values(final DefaultListModel<T> model, final List<T> values) {
        for (final T v : values) {
            model.addElement(v);
        }
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
