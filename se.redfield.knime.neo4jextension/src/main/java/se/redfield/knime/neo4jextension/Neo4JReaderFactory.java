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
public class Neo4JReaderFactory extends NodeFactory<Neo4JReaderModel> {
    /**
     * Default constructor.
     */
    public Neo4JReaderFactory() {
        super();
    }

    @Override
    public Neo4JReaderModel createNodeModel() {
        return new Neo4JReaderModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }
    @Override
    public NodeView<Neo4JReaderModel> createNodeView(
            final int viewIndex, final Neo4JReaderModel nodeModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new Neo4JReaderDialog();
    }
}
