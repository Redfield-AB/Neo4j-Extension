/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

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
    private JTextArea funcDescription;

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

        addTab("Script", createScriptTab(), false);
    }

    /**
     * @return script editor page.
     */
    private JComponent createScriptTab() {
        final JSplitPane topPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topPanel.setLeftComponent(createFlowVariablesNodesAndRels());
        topPanel.setRightComponent(createScriptPanel());

        final JSplitPane tab = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        tab.setTopComponent(topPanel);
        tab.setBottomComponent(createFunctions());

        return tab;
    }
    private JPanel createScriptPanel() {
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
        return scriptPanel;
    }

    private JSplitPane createFlowVariablesNodesAndRels() {
        final JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sp.setTopComponent(createFlowVariables());

        //nodes and relationships
        final JSplitPane nodesAndRel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        nodesAndRel.setTopComponent(createNodes());
        nodesAndRel.setBottomComponent(createRelationships());

        sp.setBottomComponent(nodesAndRel);
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
    private JPanel createFunctions() {
        final JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Functions"));

        this.funcDescription = createTextAreaWithoutMinSize();
        funcDescription.setBackground(p.getBackground());
        funcDescription.setEditable(false);
        funcDescription.setLineWrap(true);
        p.add(funcDescription, BorderLayout.CENTER);

        final JList<FunctionDesc> list = createListWithHandler(functions, v -> insertToScript(v.getName()));
        list.getSelectionModel().addListSelectionListener(e -> {
            final int index = list.getSelectedIndex();
            if (index > -1) {
                final FunctionDesc el = list.getModel().getElementAt(index);
                funcDescription.setText(buildFunctionDescription(el));
            }
        });

        list.setCellRenderer(new FunctionDescRenderer());
        p.add(new JScrollPane(list), BorderLayout.WEST);

        return p;
    }
    private String buildFunctionDescription(final FunctionDesc v) {
        final StringBuilder sb = new StringBuilder("Signature:\n");
        sb.append(v.getSignature()).append("\n\n");
        sb.append("Description:\n");
        sb.append(v.getDescription());
        return sb.toString();
    }

    /**
     * @return script editor.
     */
    private JTextArea createScriptEditor() {
        final JTextArea scriptEditor = createTextAreaWithoutMinSize();

        final UndoManager undoRedo = new UndoManager();
        scriptEditor.getDocument().addUndoableEditListener(
            new UndoableEditListener() {
                @Override
                public void undoableEditHappened(final UndoableEditEvent e) {
                    undoRedo.addEdit(e.getEdit());
                }
            });

        final KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(
                KeyEvent.VK_Z, Event.CTRL_MASK);
        final KeyStroke redoKeyStroke = KeyStroke.getKeyStroke(
                KeyEvent.VK_Z, Event.CTRL_MASK | Event.SHIFT_MASK);

        scriptEditor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(undoKeyStroke, "undoKeyStroke");
        scriptEditor.getActionMap().put("undoKeyStroke", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    undoRedo.undo();
                } catch (final CannotUndoException cue) {
                }
            }
        });

        // Map redo action
        scriptEditor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(redoKeyStroke, "redoKeyStroke");
        scriptEditor.getActionMap().put("redoKeyStroke", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    undoRedo.redo();
                } catch (final CannotRedoException cre) {
                }
            }
        });
        scriptEditor.setLineWrap(true);
        return scriptEditor;
    }
    private JTextArea createTextAreaWithoutMinSize() {
        return new JTextArea() {
            private static final long serialVersionUID = -6583141839191451218L;
            @Override
            public Dimension getMinimumSize() {
                //allways return minimum width as zero
                final Dimension min = super.getMinimumSize();
                return min == null ? new Dimension() : new Dimension(0, min.height);
            }
        };
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
        funcDescription.setText("");
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
