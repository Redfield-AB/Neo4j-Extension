/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;

import se.redfield.knime.neo4j.connector.ConnectorPortObject;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderFactory extends ConfigurableNodeFactory<ReaderModel> {
    /**
     * Default constructor.
     */
    public ReaderFactory() {
        super();
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return new ReaderDialog();
    }
    @Override
    protected ReaderModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new ReaderModel(creationConfig);
    }
    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder builder = new PortsConfigurationBuilder();

        builder.addOptionalInputPortGroup("Input table", BufferedDataTable.TYPE);
        builder.addFixedInputPortGroup("Neo4j input connection", ConnectorPortObject.TYPE);

        builder.addFixedOutputPortGroup("Table/JSON response", BufferedDataTable.TYPE);
        builder.addFixedOutputPortGroup("Neo4j output connection", ConnectorPortObject.TYPE);
        return Optional.of(builder);
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
}
