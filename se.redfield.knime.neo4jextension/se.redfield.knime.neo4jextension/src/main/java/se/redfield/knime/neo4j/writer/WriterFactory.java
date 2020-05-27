/**
 *
 */
package se.redfield.knime.neo4j.writer;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class WriterFactory extends NodeFactory<WriterModel> {
    /**
     * Default constructor.
     */
    public WriterFactory() {
        super();
    }

    @Override
    public WriterModel createNodeModel() {
        return new WriterModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }
    @Override
    public NodeView<WriterModel> createNodeView(
            final int viewIndex, final WriterModel nodeModel) {
        throw new UnsupportedOperationException();
    }
    @Override
    protected boolean hasDialog() {
        return true;
    }
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new WriterDialog();
    }
}
