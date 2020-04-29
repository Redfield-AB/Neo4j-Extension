/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;

import se.redfield.knime.neo4j.connector.ConnectorPortData;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfig;
import se.redfield.knime.neo4j.reader.cfg.ReaderConfigSerializer;
import se.redfield.knime.ui.AlwaysVisibleCaret;
import se.redfield.knime.ui.ReaderLabel;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderDialog extends DataAwareNodeDialogPane {
    private final JCheckBox useJsonOutput = new JCheckBox();
    private JTextArea scriptEditor;

    private JTree labelsTree = new JTree();
    private JSplitPane sourcesContainer;
    final DefaultMutableTreeNode labelsTreeRoot = new DefaultMutableTreeNode();

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

        p.add(north, BorderLayout.NORTH);

        //Script editor
        scriptEditor = new JTextArea() {
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

        //Labels tree
        labelsTree.setModel(new DefaultTreeModel(labelsTreeRoot));
        labelsTree.setRootVisible(false);
        labelsTree.setEditable(false);
        labelsTree.setSelectionModel(null);
        labelsTree.setFocusable(false);
        labelsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();
                    mouseClickedOnLabelsTree(e.getX(), e.getY());
                }
            }
        });

        //Node label selection
        sourcesContainer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        sourcesContainer.setLeftComponent(labelsTree);
        sourcesContainer.setRightComponent(scriptEditor);
        p.add(sourcesContainer, BorderLayout.CENTER);

        return p;
    }

    /**
     * @param x mouse x.
     * @param y mouse y.
     */
    protected void mouseClickedOnLabelsTree(final int x, final int y) {
        final TreePath path = labelsTree.getPathForLocation(x, y);
        if (path != null) {
            final Object last = path.getLastPathComponent();
            if (last instanceof DefaultMutableTreeNode) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) last;
                if (node.isLeaf() && node.getUserObject() instanceof ReaderLabel) {
                    addStringToCurrentScriptEditorPosition((ReaderLabel) node.getUserObject());
                }
            }
        }
    }
    /**
     * @param label label to insert.
     */
    private void addStringToCurrentScriptEditorPosition(final ReaderLabel label) {
        String text;
        switch (label.getType()) {
            case NodeLabel:
                text = "(:" + label.getText() + ")";
                break;
            case RelationshipType:
                text = "-[:" + label.getText() + "]-";
                break;
            default:
            case PropertyKey:
                text = label.getText();
                break;
        }

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

        //add labels and nodes
        labelsTreeRoot.removeAllChildren();

        //node labels
        final DefaultMutableTreeNode nodeLabels = new DefaultMutableTreeNode("Node labels:");
        addLiefs(nodeLabels, data.getNodeLabels(), ReaderLabel.Type.NodeLabel);
        labelsTreeRoot.add(nodeLabels);

        //relationship types
        final DefaultMutableTreeNode relTypes = new DefaultMutableTreeNode("Relationship types:");
        addLiefs(relTypes, data.getRelationshipTypes(), ReaderLabel.Type.RelationshipType);
        labelsTreeRoot.add(relTypes);

        //Property keys
        final DefaultMutableTreeNode propKeys = new DefaultMutableTreeNode("Properthy keys:");
        addLiefs(propKeys, data.getPropertyKeys(), ReaderLabel.Type.PropertyKey);
        labelsTreeRoot.add(propKeys);

        final DefaultTreeModel treeModel = (DefaultTreeModel) labelsTree.getModel();
        treeModel.nodeChanged(nodeLabels);
        treeModel.nodeChanged(relTypes);
        treeModel.nodeChanged(propKeys);

        labelsTree.expandPath(new TreePath(treeModel.getPathToRoot(nodeLabels)));
        labelsTree.expandPath(new TreePath(treeModel.getPathToRoot(relTypes)));
        labelsTree.expandPath(new TreePath(treeModel.getPathToRoot(propKeys)));
    }

    /**
     * @param parent
     * @param liefs
     */
    private void addLiefs(final DefaultMutableTreeNode parent,
            final List<String> liefs, final ReaderLabel.Type type) {
        for (final String str : liefs) {
            final ReaderLabel label = new ReaderLabel(type);
            label.setText(str);

            final DefaultMutableTreeNode n = new DefaultMutableTreeNode(label);
            parent.add(n);
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
