/**
 *
 */
package se.redfield.knime.neo4j.reader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderFactory extends NodeFactory<ReaderModel> {
    /**
     * Default constructor.
     */
    public ReaderFactory() {
        super();
    }

    @Override
    public ReaderModel createNodeModel() {
        return new ReaderModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }
    @Override
    public NodeView<ReaderModel> createNodeView(
            final int viewIndex, final ReaderModel nodeModel) {
        throw new UnsupportedOperationException();
    }
    @Override
    protected boolean hasDialog() {
        return true;
    }
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ReaderDialog();
    }
}
