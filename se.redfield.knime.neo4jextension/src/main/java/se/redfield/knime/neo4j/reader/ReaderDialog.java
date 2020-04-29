/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.swing.SwingUtilities;
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
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import se.redfield.knime.neo4j.connector.ConnectorPortData;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.db.Neo4JSupport;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfig;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfigSerializer;
import se.redfield.knime.ui.AlwaysVisibleCaret;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderDialog extends DataAwareNodeDialogPane {
    private final JCheckBox useJsonOutput = new JCheckBox();
    private JTextArea scriptEditor;

    private JSplitPane sourcesContainer;

    private DefaultListModel<String> flowVariables = new DefaultListModel<>();
    private DefaultListModel<String> nodes = new DefaultListModel<>();
    private DefaultListModel<String> nodeProperties = new DefaultListModel<>();
    private DefaultListModel<String> relationships = new DefaultListModel<>();
    private DefaultListModel<String> functions = new DefaultListModel<>();
    private Neo4JSupport support;

    /**
     * Default constructor.
     */
    public ReaderDialog() {
        super();

        final JPanel scriptPanel = createScriptPage();
        final JScrollPane sp = new JScrollPane(scriptPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        addTab("Script", sp, false);
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
        scriptPanel.add(scriptEditor, BorderLayout.CENTER);

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
        final JSplitPane p = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        p.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Node labels"));

        final JList<String> nodesList = createListWithHandler(nodes, v -> insertToScript("(:" + v + ")"));
        nodesList.addListSelectionListener(e -> loadNodeProperties(
                nodesList.getModel().getElementAt(nodesList.getSelectedIndex())));

        p.setLeftComponent(nodesList);
        p.setRightComponent(createListWithHandler(this.nodeProperties, v -> insertToScript(v)));
        return p;
    }

    private JComponent createFlowVariables() {
        return createTitledList("Flow variables", flowVariables, v -> insertToScript(v));
    }
    private JPanel createTitledList(final String title,
            final DefaultListModel<String> listModel, final ValueInsertHandler h) {
        final JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), title));

        final JList<String> list = createListWithHandler(listModel, h);
        p.add(list, BorderLayout.CENTER);
        return p;
    }

    private JList<String> createListWithHandler(final DefaultListModel<String> listModel,
            final ValueInsertHandler h) {
        final JList<String> list = new JList<String>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> listSelectionChanged(list, h));
        return list;
    }
    private void listSelectionChanged(final JList<String> list, final ValueInsertHandler h) {
        final int index = list.getSelectedIndex();
        if (index > -1) {
            //remove old mouse listener
            final MouseListener[] listeners = list.getMouseListeners();
            for (final MouseListener l : listeners) {
                if (l instanceof ListClickListener) {
                    list.removeMouseListener(l);
                }
            }

            //invoke later for avoid of immediately triggering.
            SwingUtilities.invokeLater(
                    () -> list.addMouseListener(new ListClickListener(list, h, index)));
        }
    }

    private Component createRelationshipsAndFunctions() {
        final JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sp.setTopComponent(createRelationships());
        sp.setBottomComponent(createFunctions());
        return sp;
    }
    private JPanel createRelationships() {
        return createTitledList("Relationship types", relationships,
                v -> insertToScript("-[:" + v + "]-"));
    }
    private JPanel createFunctions() {
        return createTitledList("Functions", functions,
                v -> insertToScript(v));
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

        final AlwaysVisibleCaret caret = new AlwaysVisibleCaret();
        caret.setBlinkRate(500);
        scriptEditor.setCaret(caret);
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
        this.support = new Neo4JSupport(data.getConnectorConfig());

        scriptEditor.setText(model.getScript());
        useJsonOutput.setSelected(model.isUseJson());

        //clear all lists
        flowVariables.clear();
        nodes.clear();
        nodeProperties.clear();
        relationships.clear();
        functions.clear();

        final Set<VariableType<?>> types = new HashSet<>();
        types.add(VariableType.BooleanArrayType.INSTANCE);
        types.add(VariableType.BooleanType.INSTANCE);
        types.add(VariableType.CredentialsType.INSTANCE);
        types.add(VariableType.DoubleArrayType.INSTANCE);
        types.add(VariableType.BooleanArrayType.INSTANCE);
        types.add(VariableType.DoubleArrayType.INSTANCE);
        types.add(VariableType.DoubleType.INSTANCE);
        types.add(VariableType.IntArrayType.INSTANCE);
        types.add(VariableType.IntType.INSTANCE);
        types.add(VariableType.LongArrayType.INSTANCE);
        types.add(VariableType.LongType.INSTANCE);
        types.add(VariableType.StringArrayType.INSTANCE);
        types.add(VariableType.StringType.INSTANCE);

        final Map<String, FlowVariable> vars = getAvailableFlowVariables(
                types.toArray(new VariableType[types.size()]));
        for (final String varName : vars.keySet()) {
            flowVariables.addElement(varName);
        }

        values(nodes, data.getNodeLabels());
        values(relationships, data.getRelationshipTypes());
    }

    private void loadNodeProperties(final String label) {
        support.runAsync(s -> addPropertyKeys(s, label));
    }
    private Void addPropertyKeys(final Session s, final String label) {
        nodeProperties.clear();

        final StringBuilder query = new StringBuilder("MATCH (n:"
                + label
                + ")\n");
        query.append("WITH KEYS(n) AS keys\n");
        query.append("UNWIND keys AS key\n");
        query.append("RETURN DISTINCT key\n");
        query.append("ORDER BY key\n");

        final Map<String, Object> params = new HashMap<>();
        params.put("label", label);
        final List<Record> result = s.readTransaction(tx -> tx.run(query.toString(), params).list());

        final List<String> props = new LinkedList<>();
        for (final Record r : result) {
            props.add(r.get(0).asString());
        }

        //add properties in event dispatch thread
        SwingUtilities.invokeLater(() -> {
            nodeProperties.clear();
            for (final String p : props) {
                this.nodeProperties.addElement(p);
            }
        });
        return null;
    }

    private void values(final DefaultListModel<String> model, final List<String> values) {
        for (final String v : values) {
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
