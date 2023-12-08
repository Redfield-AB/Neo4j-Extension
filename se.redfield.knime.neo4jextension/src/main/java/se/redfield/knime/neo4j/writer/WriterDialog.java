/**
 *
 */
package se.redfield.knime.neo4j.writer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.ButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

import se.redfield.knime.neo4j.connector.ConnectorConfig;
import se.redfield.knime.neo4j.connector.ConnectorSpec;
import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.Named;
import se.redfield.knime.neo4j.connector.NamedWithProperties;
import se.redfield.knime.neo4j.db.LabelsAndFunctions;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.model.FlowVariablesProvider;
import se.redfield.knime.neo4j.model.ModelUtils;
import se.redfield.knime.neo4j.ui.BatchNamedValueRenderer;
import se.redfield.knime.neo4j.ui.BatchPattern;
import se.redfield.knime.neo4j.ui.BatchWriterPattern;
import se.redfield.knime.neo4j.ui.NamedValueRenderer;
import se.redfield.knime.neo4j.ui.OnClickInserter;
import se.redfield.knime.neo4j.ui.SplitPanelExt;
import se.redfield.knime.neo4j.ui.StringRenderer;
import se.redfield.knime.neo4j.ui.UiUtils;
import se.redfield.knime.neo4j.ui.ValueInsertHandler;
import se.redfield.knime.neo4j.ui.WithStringIconCellRenderer;
import se.redfield.knime.neo4j.ui.editor.CypherEditor;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class WriterDialog extends NodeDialogPane implements FlowVariablesProvider {
    private static final String KEEP_ORIGIN_SOURCE_ROWS_ORDER = "Keep original row order";
    private static final String STOP_ON_QUERY_FAILURE = "Stop on query failure";
    private static final String INPUT_COLUMN_TAB = "Query from table";
    private static final String SCRIPT_TAB = "Script";
    private static final String COLUMN_QUERY = "Column with query";
    private static final String USE_ASYNC_EXE = "Use asynchronous query execution";
    private static final String BATCH_TAB = "Batch query";
    private static final String USE_BATCH = "Use batch request";
    private static final String BATCH_PARAMETER_NAME = "Name for batch parameter";

    private final ToggleButtonModel useAsyncExe = new ToggleButtonModel();
    private final ToggleButtonModel useAsyncExeInBatchTab = new ToggleButtonModel();
    private final JComboBox<String> inputColumn = new JComboBox<>(new DefaultComboBoxModel<>());
    private final ToggleButtonModel keepSourceOrder = new ToggleButtonModel();
    private final ToggleButtonModel keepSourceOrderInBatchTab = new ToggleButtonModel();
    private final ToggleButtonModel stopInQueryFailure = new ToggleButtonModel();
    private final ToggleButtonModel stopInQueryFailureInBatchTab = new ToggleButtonModel();
    private final ToggleButtonModel useBatchQuery = new ToggleButtonModel();

    private CypherEditor scriptEditor;
    private CypherEditor batchScriptEditor;
    private JTextArea funcDescription;
    private JTextArea descriptionArea;
	private JTextField batchParameterName;

    private final JList<BatchPattern> batchPatterns = new JList<>(new DefaultListModel<>());
    private final JList<FlowVariable> flowVariables = new JList<>(new DefaultListModel<>());
    private final JList<NamedWithProperties> nodes = new JList<>(new DefaultListModel<>());
    private final JList<NamedWithProperties> relationships = new JList<>(new DefaultListModel<>());
    private final JList<FunctionDesc> functions = new JList<>(new DefaultListModel<>());
    private final JList<String> inputColumnsJList = new JList<>(new DefaultListModel<>());

    private final DefaultListModel<String> nodeProperties = new DefaultListModel<>();
    private final DefaultListModel<String> relationshipsProperties = new DefaultListModel<>();

    private Dimension savedSize;

    private ConnectorConfig connector;
    private boolean useInputTable;
    private WriterConfig oldModel;

    /**
     * Default constructor.
     */
    public WriterDialog() {
        super();

        addTab(SCRIPT_TAB, createScriptTab(), false);
        addTab(INPUT_COLUMN_TAB, createInputColumnTab(), false);
        addTab(BATCH_TAB, createBatchTab(), false);
    }

    private JPanel createInputColumnTab() {
        final JPanel tab = new JPanel(new BorderLayout());

        final JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final JPanel parent = new JPanel(new GridBagLayout());
        wrapper.add(parent);

        // Use batch script
        final JCheckBox cbUseBatch = createCheckBoxFromButtonModel(useBatchQuery);
        addLabeledComponent(parent, USE_BATCH, cbUseBatch, 0);

        inputColumn.setRenderer(new WithStringIconCellRenderer());
        addLabeledComponent(parent, COLUMN_QUERY, this.inputColumn, 1);

        final JCheckBox cbStopOnFailure = createCheckBoxFromButtonModel(stopInQueryFailure);
        addLabeledComponent(parent, STOP_ON_QUERY_FAILURE, cbStopOnFailure, 2);
        cbStopOnFailure.addChangeListener(e -> stopInQueryFailureInBatchTab.setSelected(cbStopOnFailure.isSelected()));

        final JCheckBox cbUseAsync = createCheckBoxFromButtonModel(useAsyncExe);
        addLabeledComponent(parent, USE_ASYNC_EXE, cbUseAsync, 3);

        final JCheckBox cbKeepSourceOrder = createCheckBoxFromButtonModel(keepSourceOrder);
        addLabeledComponent(parent, KEEP_ORIGIN_SOURCE_ROWS_ORDER, cbKeepSourceOrder, 5);
        cbKeepSourceOrder.addChangeListener(e -> keepSourceOrderInBatchTab.setSelected(cbKeepSourceOrder.isSelected()));

        cbUseAsync.addChangeListener(e -> {
            cbKeepSourceOrder.setEnabled(useAsyncExe.isSelected() && !useBatchQuery.isSelected());
            useAsyncExe.setSelected(cbUseAsync.isSelected());
        });

        tab.add(wrapper, BorderLayout.CENTER);

        useBatchQuery.addChangeListener(e -> {
            inputColumn.setEnabled(!useBatchQuery.isSelected());
            cbStopOnFailure.setEnabled(!useBatchQuery.isSelected());
            cbUseAsync.setEnabled(!useBatchQuery.isSelected());
            cbKeepSourceOrder.setEnabled(!useBatchQuery.isSelected() && useAsyncExe.isSelected());
        });
        return tab;
    }
    private void addLabeledComponent(final JPanel container, final String label,
            final JComponent component, final int row) {
        //add label
        final GridBagConstraints lc = new GridBagConstraints();
        lc.fill = GridBagConstraints.HORIZONTAL;
        lc.gridx = 0;
        lc.gridy = row;
        lc.weightx = 0.;

        final JLabel l = new JLabel(label);
        l.setHorizontalTextPosition(SwingConstants.RIGHT);
        l.setHorizontalAlignment(SwingConstants.RIGHT);

        final JPanel labelWrapper = new JPanel(new BorderLayout());
        labelWrapper.setBorder(new EmptyBorder(0, 0, 0, 5));
        labelWrapper.add(l, BorderLayout.CENTER);
        container.add(labelWrapper, lc);

        //add component.
        final GridBagConstraints cc = new GridBagConstraints();
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.gridx = 1;
        cc.gridy = row;
        cc.weightx = 1.;
        container.add(component, cc);
    }

    /**
     * @return script editor page.
     */
    private JComponent createScriptTab() {
        final JPanel scriptPanel = createScriptPanel();
        final JSplitPane fv = createFlowVariablesNodesAndRels(this.scriptEditor);

        final JPanel p = new JPanel(new BorderLayout());
        p.add(fv, BorderLayout.CENTER);

        final JSplitPane topPanel = createSplitPane(JSplitPane.HORIZONTAL_SPLIT, 0.3);
        topPanel.setLeftComponent(p);
        topPanel.setRightComponent(scriptPanel);

        final JSplitPane vertical = createSplitPane(JSplitPane.VERTICAL_SPLIT, 0.67);
        vertical.setTopComponent(topPanel);
        vertical.setBottomComponent(createFunctions(this.scriptEditor));

        return vertical;
    }
    private JPanel createScriptPanel() {
        //stop on failure
        final JPanel stopOnFailurePane = new JPanel(new BorderLayout(5, 5));
        stopOnFailurePane.add(new JLabel(STOP_ON_QUERY_FAILURE), BorderLayout.WEST);
        final JCheckBox cb = new JCheckBox();
        cb.setModel(stopInQueryFailure);
        stopOnFailurePane.add(cb, BorderLayout.CENTER);

        final JPanel north = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        north.add(stopOnFailurePane);

        //Script editor
        final JPanel scriptPanel = new JPanel(new BorderLayout());
        scriptPanel.add(north, BorderLayout.NORTH);
		this.scriptEditor = new CypherEditor();

		scriptPanel.add(scriptEditor.getComponent(), BorderLayout.CENTER);
        return scriptPanel;
    }

    private JComponent createBatchTab() {
        // Batch script editor
		this.batchScriptEditor = new CypherEditor();
        useBatchQuery.addChangeListener(e -> batchScriptEditor.setEnabled(useBatchQuery.isSelected()));

        final JSplitPane leftPanel = createSplitPane(JSplitPane.VERTICAL_SPLIT, 0.3);
        leftPanel.setTopComponent(createSettingsPanel());
        leftPanel.setBottomComponent(createFlowVariablesNodesAndRels(batchScriptEditor));

        final JSplitPane topPanel = createSplitPane(JSplitPane.HORIZONTAL_SPLIT, 0.35);
        topPanel.setTopComponent(leftPanel);
		topPanel.setBottomComponent(batchScriptEditor.getComponent());

        final JSplitPane batchTab = createSplitPane(JSplitPane.VERTICAL_SPLIT, 0.78);
        batchTab.setTopComponent(topPanel);
        batchTab.setBottomComponent(createBottomPanelInBatchTab());

        return batchTab;
    }

    private JPanel createSettingsPanel() {
        final JPanel settingsWrapper = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsWrapper.add(settingsPanel);

        // Use batch script
        final JCheckBox cbUseBatch = createCheckBoxFromButtonModel(useBatchQuery);
        addLabeledComponent(settingsPanel, USE_BATCH, cbUseBatch, 0);

        // Stop on failure
        final JCheckBox cbStopOnFailure = createCheckBoxFromButtonModel(stopInQueryFailureInBatchTab);
        addLabeledComponent(settingsPanel, STOP_ON_QUERY_FAILURE, cbStopOnFailure,1);
        cbStopOnFailure.addChangeListener(e -> stopInQueryFailure.setSelected(cbStopOnFailure.isSelected()));

        // Use asynchronous query execution
        final JCheckBox cbUseAsync = createCheckBoxFromButtonModel(useAsyncExeInBatchTab);
        addLabeledComponent(settingsPanel, USE_ASYNC_EXE, cbUseAsync, 2);

        // Keep original row order
        final JCheckBox cbKeepSourceOrder = createCheckBoxFromButtonModel(keepSourceOrderInBatchTab);
        addLabeledComponent(settingsPanel, KEEP_ORIGIN_SOURCE_ROWS_ORDER, cbKeepSourceOrder, 3);
        cbKeepSourceOrder.addChangeListener(e -> keepSourceOrder.setSelected(cbKeepSourceOrder.isSelected()));

        cbUseAsync.addChangeListener(e -> {
            cbKeepSourceOrder.setEnabled(useAsyncExe.isSelected() && useBatchQuery.isSelected());
            useAsyncExe.setSelected(cbUseAsync.isSelected());
        });

        // Name for batch parameter
		this.batchParameterName = new JTextField(20);
		addLabeledComponent(settingsPanel, BATCH_PARAMETER_NAME, batchParameterName, 4);

        useBatchQuery.addChangeListener(e -> {
            if (useInputTable){
                cbStopOnFailure.setEnabled(useBatchQuery.isSelected());
                cbUseAsync.setEnabled(useBatchQuery.isSelected());
                cbKeepSourceOrder.setEnabled(useBatchQuery.isSelected() && useAsyncExe.isSelected());
                batchParameterName.setEnabled(useBatchQuery.isSelected());
            }
        });

        return settingsWrapper;
    }

    private JSplitPane createBottomPanelInBatchTab(){
        final JPanel batchPatternsPanel = new JPanel(new BorderLayout());
        batchPatternsPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Batch Patterns"));
        final JPanel functionPanel = new JPanel(new BorderLayout());
        functionPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Functions"));
        final JPanel patternFunctionPanel = new JPanel(new BorderLayout());
        patternFunctionPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED)));
        final JPanel columnListPanel = new JPanel(new BorderLayout());
        columnListPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Column List"));

        descriptionArea = createTextAreaWithoutMinSize();
        descriptionArea.setBackground(batchPatternsPanel.getBackground());
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);

        // batch patterns
        settingsJList(
                batchPatterns,
				new OnClickInserter<>(batchPatterns, v -> batchScriptEditor.insert(v.getScript())),
                e -> {
                    final int index = batchPatterns.getSelectedIndex();
                    if (index > -1) {
                        final BatchPattern el = batchPatterns.getModel().getElementAt(index);
                        descriptionArea.setText(buildBatchPatternDescription(el));
                    }
                },
                new BatchNamedValueRenderer()
        );

        // functions
        JList<FunctionDesc> functionList = new JList<>(functions.getModel());
        settingsJList(
                functionList,
                new OnClickInserter<>(functionList,
                        v -> {
                            if (useInputTable) {
								batchScriptEditor.insert(v.getName());
                            }
                        }),
                e -> {
                    final int index = functionList.getSelectedIndex();
                    if (index > -1 && useInputTable) {
                        final FunctionDesc el = functionList.getModel().getElementAt(index);
                        descriptionArea.setText(buildFunctionDescription(el));
                    }
                },
                new NamedValueRenderer()
        );

        // inputColumns
        settingsJList(
                inputColumnsJList,
				new OnClickInserter<>(inputColumnsJList, batchScriptEditor::insert),
                e -> {},
                new StringRenderer()
        );

        batchPatternsPanel.add(new JScrollPane(batchPatterns));
        functionPanel.add(new JScrollPane(functionList));
        patternFunctionPanel.add(batchPatternsPanel, BorderLayout.WEST);
        patternFunctionPanel.add(functionPanel);

        columnListPanel.add(new JScrollPane(inputColumnsJList));

        final JSplitPane pfDesk = createSplitPane(JSplitPane.HORIZONTAL_SPLIT, 0.5);
        pfDesk.setTopComponent(patternFunctionPanel);
        pfDesk.setBottomComponent(descriptionArea);

        final JSplitPane panel = createSplitPane(JSplitPane.HORIZONTAL_SPLIT, 0.9);
        panel.setTopComponent(pfDesk);
        panel.setBottomComponent(columnListPanel);

        useBatchQuery.addChangeListener(e -> {
            if (useInputTable){
                batchPatterns.setEnabled(useBatchQuery.isSelected());
                inputColumnsJList.setEnabled(useBatchQuery.isSelected());
                functionList.setEnabled(useBatchQuery.isSelected());
            } else {
                functionList.setEnabled(true);
            }
        });

        return panel;
    }

    private JCheckBox createCheckBoxFromButtonModel(ButtonModel buttonModel) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setModel(buttonModel);
        return checkBox;
    }

    private JPanel createRefreshButton() {
        final ImageIcon icon = createRefreshIcon();
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

        useBatchQuery.addChangeListener(e -> {
            if (useInputTable) {
                b.setEnabled(useBatchQuery.isSelected());
            } else {
                b.setEnabled(true);
            }
        });

        return p;
    }

    /**
     * @return image icon
     */
    protected ImageIcon createRefreshIcon() {
        return UiUtils.createRefreshIcon();
    }
    private JSplitPane createFlowVariablesNodesAndRels(CypherEditor scriptEditor) {
        final JSplitPane fvAndReButton = createSplitPane(JSplitPane.HORIZONTAL_SPLIT, 0.9);
        fvAndReButton.setTopComponent(createFlowVariables(scriptEditor));
        fvAndReButton.setBottomComponent(createRefreshButton());

        //nodes and relationships
        final JSplitPane nodesAndRel = createSplitPane(JSplitPane.VERTICAL_SPLIT, 0.5);
        nodesAndRel.setTopComponent(createNodes(scriptEditor));
        nodesAndRel.setBottomComponent(createRelationships(scriptEditor));

        final JSplitPane sp = createSplitPane(JSplitPane.VERTICAL_SPLIT, 0.2);
        sp.setTopComponent(fvAndReButton);
        sp.setBottomComponent(nodesAndRel);

        return sp;
    }

    private JSplitPane createSplitPane(final int orientation,
            final double dividerPosition) {
        final JSplitPane sp = new SplitPanelExt(orientation);
        sp.setDividerLocation(dividerPosition);
        return sp;
    }

	private JSplitPane createNodes(CypherEditor scriptEditor) {
        return createNamedWithPropertiesComponent(
                new JList<>(nodes.getModel()), nodeProperties,
                "Node labels", "Node properties",
				v -> scriptEditor.insert(v.getName()), scriptEditor);
    }

	private JSplitPane createRelationships(CypherEditor scriptEditor) {
        return createNamedWithPropertiesComponent(
                new JList<>(relationships.getModel()), relationshipsProperties,
                "Relationship labels", "Relationship properties",
				v -> scriptEditor.insert(v.getName()), scriptEditor);
    }

    private JSplitPane createNamedWithPropertiesComponent(final JList<NamedWithProperties> named,
            final DefaultListModel<String> propsOfNamed, final String title,
            final String propertiesTitle, final ValueInsertHandler<NamedWithProperties> handler,
			CypherEditor scriptEditor) {
        final JSplitPane p = createSplitPane(JSplitPane.HORIZONTAL_SPLIT, 0.5);

        final JPanel nodesContainer = new JPanel(new BorderLayout());
        nodesContainer.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), title));

        named.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        named.addMouseListener(new OnClickInserter<NamedWithProperties>(named, handler));
        nodesContainer.add(named, BorderLayout.CENTER);

        named.setCellRenderer(new NamedValueRenderer());
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

		final JPanel props = createTitledList(propertiesTitle, propsOfNamed, scriptEditor::insert);
        p.setRightComponent(props);

        useBatchQuery.addChangeListener(e -> {
            if (useInputTable) {
                named.setEnabled(useBatchQuery.isSelected());
            } else {
                named.setEnabled(true);
            }
        });
        return p;
    }

    private JComponent createFlowVariables(CypherEditor scriptEditor) {
        final JList<FlowVariable> flowVariableJList = new JList<>(flowVariables.getModel());
        final JPanel flowVariablesPanel = new JPanel(new BorderLayout());
        flowVariablesPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Flow variables"));


        settingsJList(
                flowVariableJList,
                new OnClickInserter<>(
						flowVariableJList, v -> scriptEditor.insert("${{" + v.getName() + "}}")),
                e -> {},
                new FlowVariableListCellRenderer()
        );
        flowVariablesPanel.add(new JScrollPane(flowVariableJList), BorderLayout.CENTER);

        useBatchQuery.addChangeListener(e -> {
            if (useInputTable) {
                flowVariableJList.setEnabled(useBatchQuery.isSelected());
            } else {
                flowVariableJList.setEnabled(true);
            }
        });
        return flowVariablesPanel;
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
        list.addMouseListener(new OnClickInserter<T>(list, h));

        useBatchQuery.addChangeListener(e -> {
            if (useInputTable) {
                list.setEnabled(useBatchQuery.isSelected());
            } else {
                list.setEnabled(true);
            }
        });
        return list;
    }

	private JPanel createFunctions(CypherEditor scriptEditor) {
        final JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Functions"));

        this.funcDescription = createTextAreaWithoutMinSize();
        funcDescription.setBackground(p.getBackground());
        funcDescription.setEditable(false);
        funcDescription.setLineWrap(true);
        p.add(funcDescription, BorderLayout.CENTER);

        JList<FunctionDesc> functionList = new JList<>(functions.getModel());
        settingsJList(
                functionList,
                new OnClickInserter<FunctionDesc>(functionList,
                        v -> {
                            if (!useInputTable) {
								scriptEditor.insert(v.getName());
                            }
                        }),
                e -> {
                    final int index = functionList.getSelectedIndex();
                    if (index > -1 && !useInputTable) {
                        final FunctionDesc el = functionList.getModel().getElementAt(index);
                        funcDescription.setText(buildFunctionDescription(el));
                    }
                },
                new NamedValueRenderer()
        );
        p.add(new JScrollPane(functionList), BorderLayout.WEST);

        return p;
    }
    private String buildFunctionDescription(final FunctionDesc v) {
        final StringBuilder sb = new StringBuilder("Signature:\n");
        sb.append(v.getSignature()).append("\n\n");
        sb.append("Description:\n");
        sb.append(v.getDescription());
        return sb.toString();
    }

    private String buildBatchPatternDescription(final BatchPattern v) {
        final StringBuilder sb = new StringBuilder("Description:\n");
        sb.append(v.getDescription()).append("\n\n");
        sb.append("Script:\n");
        sb.append(v.getScript());
        return sb.toString();
    }

    private <T> void settingsJList(JList<T> list,
                                   MouseListener inserter,
                                   ListSelectionListener listener,
                                   ListCellRenderer<? super T> cellRenderer){
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(inserter);
        list.getSelectionModel().addListSelectionListener(listener);
        list.setCellRenderer(cellRenderer);
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

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final WriterConfig model = buildConfig();
        new WriterConfigSerializer().save(model, settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        DataTableSpec tableInput = null;
        final ConnectorSpec connectorPort;

        if (specs.length > 1) {
            tableInput = (DataTableSpec) specs[0];
        } else {
        }

        connectorPort = (ConnectorSpec) specs[specs.length - 1];
        //check connector is NULL
        if (connectorPort == null) {
            throw new NotConfigurableException("Not connected to Neo4j connection");
        }

        try {
            final WriterConfig model = new WriterConfigSerializer().read(settings);
            initFromModel(model, connectorPort.getPortData(), getStringColumns(tableInput), getAllColumns(tableInput));
        } catch (final Exception e) {
            getLogger().error(e);
            throw new NotConfigurableException(e.getMessage(), e);
        }
    }
    private List<String> getStringColumns(final DataTableSpec tableSpec) {
        if (tableSpec == null) {
            return null;
        }
        final List<String> columns = new LinkedList<>();
        for (final DataColumnSpec r : tableSpec) {
            if (r.getType() == StringCell.TYPE) {
                columns.add(r.getName());
            }
        }
        return columns;
    }

    private List<String> getAllColumns(final DataTableSpec tableSpecs) {
        if (tableSpecs == null) {
            return List.of();
        }
        final List<String> columns = new LinkedList<>();
        tableSpecs.stream().forEach(dcs -> columns.add(dcs.getName()));
        return columns;
    }

    @Override
    public void onOpen() {
        if (this.savedSize != null) {
            getPanel().setSize(savedSize);
            getPanel().setPreferredSize(savedSize);
        } else {
            final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            final Dimension dim = new Dimension(screen.width * 2 / 3, screen.height * 2 / 3);
            getPanel().setSize(dim);
            getPanel().setPreferredSize(dim);
        }
    }

    @Override
    public void onClose() {
        this.savedSize = getPanel().getSize();
    }

    /**
     * @param model model.
     * @param inputStringColumns string columns from input table.
     * @throws Exception
     */
    private void initFromModel(final WriterConfig model, final ConnectorConfig data,
            final List<String> inputStringColumns, final List<String> inputAllColumns) {
        this.connector = data;
        this.oldModel = model;

        scriptEditor.setText(model.getScript());
        inputColumn.removeAllItems();
        funcDescription.setText("");
        model(flowVariables).clear();

        this.useInputTable = inputStringColumns != null;
        if (useInputTable && model.isUseBatch()) {
            setSelected(BATCH_TAB);
        } else if (useInputTable) {
            setSelected(INPUT_COLUMN_TAB);
        } else {
            setSelected(SCRIPT_TAB);
        }

        setEnabled(!useInputTable, SCRIPT_TAB);
        setEnabled(useInputTable, INPUT_COLUMN_TAB);
        setEnabled(useInputTable, BATCH_TAB);

        reloadMetadata();
        if (useInputTable) {

            //init input table UI.
            final DefaultComboBoxModel<String> boxModel = (DefaultComboBoxModel<String>) inputColumn.getModel();
            final List<String> all = new LinkedList<String>(inputStringColumns);
            Collections.sort(all);

            for (final String c : all) {
                boxModel.addElement(c);
            }

            if (model.getInputColumn() != null && inputStringColumns.contains(model.getInputColumn())) {
                inputColumn.setSelectedItem(model.getInputColumn());
            }
            useAsyncExe.setSelected(model.isUseAsync());
            keepSourceOrder.setEnabled(useAsyncExe.isSelected());

            useBatchQuery.setSelected(model.isUseBatch());
            batchParameterName.setText(model.getBatchParameterName());
            batchScriptEditor.setText(model.getBatchScript());
            batchPatterns.setListData(BatchWriterPattern.values());
            this.inputColumnsJList.setListData(inputAllColumns.toArray(String[]::new));
        }

        final DefaultListModel<FlowVariable> flowVariablesModel = model(flowVariables);
        final Map<String, FlowVariable> vars = ModelUtils.getAvailableFlowVariables(this);
        for (final FlowVariable var : vars.values()) {
            flowVariablesModel.addElement(var);
        }

        stopInQueryFailure.setSelected(model.isStopOnQueryFailure());
        stopInQueryFailureInBatchTab.setSelected(model.isStopOnQueryFailure());
        keepSourceOrder.setSelected(model.isKeepSourceOrder());
        keepSourceOrderInBatchTab.setSelected(model.isKeepSourceOrder());
    }

    private void reloadMetadata() {
        new Thread("Reload metadata") {
            @Override
            public void run() {
                try {
                    final ConnectorConfig cfg = connector.createResolvedConfig(getCredentialsProvider());
                    final Neo4jSupport support = new Neo4jSupport(cfg);
                    final LabelsAndFunctions metaData = support.loadLabesAndFunctions();
                    WriterDialog.this.oldModel.setMetaData(metaData);

                    SwingUtilities.invokeLater(() -> applyMetadata(metaData));
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

		scriptEditor.installAutoComplete(metaData);
		batchScriptEditor.installAutoComplete(metaData);
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
    private WriterConfig buildConfig() throws InvalidSettingsException {
        final WriterConfig model = oldModel == null ? new WriterConfig() : oldModel.clone();
        if (useInputTable) {
            final String column = (String) inputColumn.getSelectedItem();
            if (column == null && !useBatchQuery.isSelected()) {
                getLogger().warn("Not input column selected");
            }
            model.setInputColumn(column);
            model.setUseAsync(this.useAsyncExe.isSelected());
            model.setUseBatch(useBatchQuery.isSelected());
            model.setBatchScript(batchScriptEditor.getText());
            model.setBatchParameterName(batchParameterName.getText());
        } else {
            final String script = scriptEditor.getText();
            if (script == null || script.trim().isEmpty()) {
                throw new InvalidSettingsException("Invalid script: " + script);
            }

            model.setScript(script);
        }
        model.setStopOnQueryFailure(stopInQueryFailure.isSelected());
        model.setKeepSourceOrder(keepSourceOrder.isSelected());
        return model;
    }

    private <T> DefaultListModel<T> model(final JList<T> list) {
        return (DefaultListModel<T>) list.getModel();
    }
}
