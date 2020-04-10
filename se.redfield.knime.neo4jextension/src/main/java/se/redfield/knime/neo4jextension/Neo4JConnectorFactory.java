/**
 *
 */
package se.redfield.knime.neo4jextension;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JConnectorFactory extends NodeFactory<Neo4JConnectorModel> {
    /**
     * Default constructor.
     */
    public Neo4JConnectorFactory() {
        super();
    }

    @Override
    public Neo4JConnectorModel createNodeModel() {
        Neo4JConnectorModel model = new Neo4JConnectorModel();
        model.reset();
        return model;
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }
    @Override
    public NodeView<Neo4JConnectorModel> createNodeView(
            final int viewIndex, final Neo4JConnectorModel nodeModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new Neo4JConnectorDialog();
    }
}
