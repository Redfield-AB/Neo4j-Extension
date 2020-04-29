/**
 *
 */
package se.redfield.knime.neo4j.connector;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorFactory extends NodeFactory<ConnectorModel> {
    /**
     * Default constructor.
     */
    public ConnectorFactory() {
        super();
    }

    @Override
    public ConnectorModel createNodeModel() {
        ConnectorModel model = new ConnectorModel();
        model.reset();
        return model;
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }
    @Override
    public NodeView<ConnectorModel> createNodeView(
            final int viewIndex, final ConnectorModel nodeModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ConnectorDialog();
    }
}
