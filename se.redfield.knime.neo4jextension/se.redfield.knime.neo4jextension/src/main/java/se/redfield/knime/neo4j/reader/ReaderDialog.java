/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
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

import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.Named;
import se.redfield.knime.neo4j.connector.NamedWithProperties;
import se.redfield.knime.neo4j.connector.cfg.ConnectorConfig;
import se.redfield.knime.neo4j.db.LabelsAndFunctions;
import se.redfield.knime.neo4j.db.Neo4jSupport;
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

    private final JList<FlowVariable> flowVariables = new JList<>(new DefaultListModel<>());
    private final JList<NamedWithProperties> nodes = new JList<>(new DefaultListModel<>());
    private final JList<NamedWithProperties> relationships = new JList<>(new DefaultListModel<>());
    private final JList<FunctionDesc> functions = new JList<>(new DefaultListModel<>());

    private final DefaultListModel<String> nodeProperties = new DefaultListModel<>();
    private final DefaultListModel<String> relationshipsProperties = new DefaultListModel<>();

    private final Map<JSplitPane, Double> dividerPositions = new HashMap<>();

    private Dimension savedSize;

    private ConnectorConfig connector;

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
        final JSplitPane fv = createFlowVariablesNodesAndRels();

        final JPanel p = new JPanel(new BorderLayout());
        p.add(createRefreshButton(), BorderLayout.NORTH);
        p.add(fv, BorderLayout.CENTER);

        final JSplitPane topPanel = createSplitPane(JSplitPane.HORIZONTAL_SPLIT, 0.3);
        topPanel.setLeftComponent(p);
        topPanel.setRightComponent(createScriptPanel());

        final JSplitPane vertical = createSplitPane(JSplitPane.VERTICAL_SPLIT, 0.67);
        vertical.setTopComponent(topPanel);
        vertical.setBottomComponent(createFunctions());

        return vertical;
    }
    private JPanel createScriptPanel() {
        final JPanel useJsonOutputPane = new JPanel(new BorderLayout(5, 5));
        useJsonOutputPane.add(new JLabel("Use JSON output"), BorderLayout.WEST);
        useJsonOutputPane.add(useJsonOutput, BorderLayout.CENTER);

        final JPanel north = new JPanel(new BorderLayout(5, 5));
        north.add(useJsonOutputPane, BorderLayout.WEST);

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

    private JPanel createRefreshButton() {
        final ImageIcon icon = new ImageIcon(ReaderDialog.class.getResource("refresh.png"));
        final JButton b = new JButton();
        b.setIcon(icon);

        final Dimension dim = b.getPreferredSize();
        b.setPreferredSize(new Dimension(dim.height, dim.height));

        b.addActionListener(e -> {
            try {
                reloadMetadata();
            } catch (final Exception exc) {
                getLogger().error("Failed to reload metadata: " + exc.getMessage());
            }
        });

        final JPanel p = new JPanel(new BorderLayout());
        p.add(b, BorderLayout.EAST);
        p.setBorder(new EmptyBorder(5, 5, 5, 5));
        return p;
    }
    private JSplitPane createFlowVariablesNodesAndRels() {
        final JSplitPane sp = createSplitPane(JSplitPane.VERTICAL_SPLIT, 0.1);
        sp.setTopComponent(createFlowVariables());

        //nodes and relationships
        final JSplitPane nodesAndRel = createSplitPane(JSplitPane.VERTICAL_SPLIT, 0.5);
        nodesAndRel.setTopComponent(createNodes());
        nodesAndRel.setBottomComponent(createRelationships());

        sp.setBottomComponent(nodesAndRel);
        return sp;
    }

    private JSplitPane createSplitPane(final int orientation,
            final double sliderPosition) {
        final JSplitPane sp = new JSplitPane(orientation);
        this.dividerPositions.put(sp, sliderPosition);
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

    private JSplitPane createNamedWithPropertiesComponent(final JList<NamedWithProperties> named,
            final DefaultListModel<String> propsOfNamed, final String title,
            final String propertiesTitle, final ValueInsertHandler<NamedWithProperties> handler) {
        final JSplitPane p = createSplitPane(JSplitPane.HORIZONTAL_SPLIT, 0.5);

        final JPanel nodesContainer = new JPanel(new BorderLayout());
        nodesContainer.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), title));

        named.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        named.addMouseListener(new ListClickListener<NamedWithProperties>(named, handler));
        nodesContainer.add(named, BorderLayout.CENTER);

        named.setCellRenderer(new NamedWithPropertiesRenderer());
        named.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                final int index = named.getSelectedIndex();
                if (index > -1) {
                    final NamedWithProperties selected = named.getModel().getElementAt(index);
                    propsOfNamed.clear();
                    for (final String prop : selected.getProperties()) {
                        propsOfNamed.addElement(prop);
                    }
                }
            }
        });

        nodesContainer.add(new JScrollPane(named));
        p.setLeftComponent(nodesContainer);

        final JPanel props = createTitledList(propertiesTitle, propsOfNamed, v -> insertToScript(v));
        p.setRightComponent(props);
        return p;
    }

    private JComponent createFlowVariables() {
        final JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Flow variables"));

        flowVariables.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        flowVariables.addMouseListener(new ListClickListener<FlowVariable>(
                flowVariables, v -> insertToScript("${{" + v.getName() + "}}")));
        flowVariables.setCellRenderer(new FlowVariableListCellRenderer());
        p.add(new JScrollPane(flowVariables), BorderLayout.CENTER);
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

        functions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        functions.addMouseListener(new ListClickListener<FunctionDesc>(functions,
                v -> insertToScript(v.getName())));
        functions.getSelectionModel().addListSelectionListener(e -> {
            final int index = functions.getSelectedIndex();
            if (index > -1) {
                final FunctionDesc el = functions.getModel().getElementAt(index);
                funcDescription.setText(buildFunctionDescription(el));
            }
        });

        functions.setCellRenderer(new FunctionDescRenderer());
        p.add(new JScrollPane(functions), BorderLayout.WEST);

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
        } catch (final Exception e) {
            getLogger().error(e);
            throw new NotConfigurableException(e.getMessage(), e);
        }
    }
    @Override
    public void onOpen() {
        if (this.savedSize != null) {
            getPanel().setSize(savedSize);
            getPanel().setPreferredSize(savedSize);
        }

        final List<JSplitPane> splitPanels = new LinkedList<>();
        getSplitPanesOrderedByParentness(splitPanels, getPanel());

        if (!splitPanels.isEmpty()) {
            UiUtils.launchOnParentWindowOpened(getPanel(), () -> {
                final JSplitPane first = splitPanels.get(0);
                if (first.isShowing() && first.isValid()
                        && first.getWidth() != 0
                        && first.getHeight() != 0) {
                    for (final JSplitPane p : splitPanels) {
                        p.setDividerLocation(dividerPositions.get(p));
                        p.doLayout();
                    }
                    return false;
                }

                //resubmit
                return true;
            });
        }
    }

    private void getSplitPanesOrderedByParentness(final List<JSplitPane> splitPanels, final Container con) {
        final Component[] children = con.getComponents();
        for (final Component c : children) {
            if (dividerPositions.containsKey(c)) {
                splitPanels.add((JSplitPane) c);
            }
            if (c instanceof Container) {
                getSplitPanesOrderedByParentness(splitPanels, (Container) c);
            }
        }
    }

    @Override
    public void onClose() {
        this.savedSize = getPanel().getSize();

        //save divider positions
        for (final JSplitPane p : new HashSet<>(dividerPositions.keySet())) {
            //convert int position to double
            double pos = 0.5;

            //get panel free size
            int freeSize;
            if (p.getOrientation() == JSplitPane.VERTICAL_SPLIT) {
                freeSize = p.getHeight() - p.getDividerSize();
            } else {
                freeSize = p.getWidth() - p.getDividerSize();
            }

            if (freeSize > 0) {
                pos = (double) p.getDividerLocation() / freeSize;
            }

            dividerPositions.put(p, pos);
        }
    }

    /**
     * @param model model.
     * @throws Exception
     */
    private void initFromModel(final ReaderConfig model, final ConnectorConfig data) {
        this.connector = data;

        scriptEditor.setText(model.getScript());
        funcDescription.setText("");
        useJsonOutput.setSelected(model.isUseJson());

        reloadMetadata();

        model(flowVariables).clear();
        final DefaultListModel<FlowVariable> flowVariablesModel = model(flowVariables);
        final Map<String, FlowVariable> vars = getAvailableFlowVariables(
                ReaderModel.getFlowVariableTypes());
        for (final FlowVariable var : vars.values()) {
            flowVariablesModel.addElement(var);
        }
    }

    private void reloadMetadata() {
        new Thread("Reload metadata") {
            @Override
            public void run() {
                try {
                    final ConnectorConfig cfg = connector.createResolvedConfig(getCredentialsProvider());
                    final Neo4jSupport support = new Neo4jSupport(cfg);
                    final LabelsAndFunctions metaData = support.loadLabesAndFunctions();

                    SwingUtilities.invokeLater(() -> {
                        applyMetadata(metaData);
                    });
                } catch (final Exception e) {
                    getLogger().error("Failed to reload metadata: " + e.getMessage());
                }
            }
        }.start();
    }
    private void applyMetadata(final LabelsAndFunctions metaData) {
        nodeProperties.clear();
        values(nodes, metaData.getNodes());

        relationshipsProperties.clear();
        values(relationships, metaData.getRelationships());

        funcDescription.setText("");
        values(functions, metaData.getFunctions());
    }

    private <T extends Named> void values(final JList<T> list, final List<T> values) {
        final T selected = list.getSelectedValue();

        final DefaultListModel<T> model = model(list);
        model.removeAllElements();

        int selectedIndex = -1;
        int index = 0;
        for (final T v : values) {
            model.addElement(v);
            if (v != null && selected != null && Objects.equals(v.getName(), selected.getName())) {
                selectedIndex = index;
            }

            index++;
        }

        if (selectedIndex > -1) {
            list.setSelectedIndex(selectedIndex);
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
    private <T> DefaultListModel<T> model(final JList<T> list) {
        return (DefaultListModel<T>) list.getModel();
    }
}
