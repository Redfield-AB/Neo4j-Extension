/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.awt.BorderLayout;
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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;

import se.redfield.knime.neo4jextension.cfg.ConnectorPortData;
import se.redfield.knime.neo4jextension.cfg.ReaderConfig;
import se.redfield.knime.neo4jextension.cfg.ReaderConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JReaderDialog extends DataAwareNodeDialogPane {
    private final JCheckBox useJsonOutput = new JCheckBox();
    private final JTextArea scriptEditor = new JTextArea();

    private JScrollPane scriptEditorPane;
    private JTree labelsTree = new JTree();
    private JSplitPane sourcesContainer;
    final DefaultMutableTreeNode labelsTreeRoot = new DefaultMutableTreeNode();

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

        final JPanel north = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        north.add(useJsonOutputPane);

        p.add(north, BorderLayout.NORTH);

        //Script editor
        scriptEditorPane = new JScrollPane(scriptEditor);

        //Labels tree
        labelsTree.setModel(new DefaultTreeModel(labelsTreeRoot));
        labelsTree.setRootVisible(false);
        labelsTree.setEditable(false);
        labelsTree.setSelectionModel(null);
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
        sourcesContainer.setRightComponent(scriptEditorPane);
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
                text = "-[:" + label.getText() + "]-";
                break;
            case PropertyKey:
                text = label.getText();
                break;
            case RelationshipType:
            default:
                text = ":" + label.getText();
                break;
        }

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
            initFromModel(model, ((ConnectorPortObject) input[0]).getPortData());
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

        final DefaultMutableTreeNode nodeLabels = new DefaultMutableTreeNode("Node labels:");
        addLiefs(nodeLabels, data.getNodeLabels(), ReaderLabel.Type.NodeLabel);
        labelsTreeRoot.add(nodeLabels);

        final DefaultMutableTreeNode relTypes = new DefaultMutableTreeNode("Relationship types:");
        addLiefs(relTypes, data.getRelationshipTypes(), ReaderLabel.Type.RelationshipType);
        labelsTreeRoot.add(relTypes);

        final DefaultTreeModel treeModel = (DefaultTreeModel) labelsTree.getModel();
        treeModel.nodeChanged(nodeLabels);
        treeModel.nodeChanged(relTypes);

        labelsTree.expandPath(new TreePath(treeModel.getPathToRoot(nodeLabels)));
        labelsTree.expandPath(new TreePath(treeModel.getPathToRoot(relTypes)));
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
