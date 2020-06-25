/**
 *
 */
package se.redfield.knime.neo4j.writer;

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
public class WriterFactory extends ConfigurableNodeFactory<WriterModel> {
    /**
     * Default constructor.
     */
    public WriterFactory() {
        super();
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return new WriterDialog();
    }
    @Override
    protected WriterModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new WriterModel(creationConfig);
    }
    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder builder = new PortsConfigurationBuilder();

        builder.addOptionalInputPortGroup("Input table", BufferedDataTable.TYPE);
        builder.addFixedInputPortGroup("Neo4j input connection", ConnectorPortObject.TYPE);

        builder.addFixedOutputPortGroup("Neo4j Output", BufferedDataTable.TYPE);
        builder.addFixedOutputPortGroup("Neo4j output connection", ConnectorPortObject.TYPE);
        return Optional.of(builder);
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
}
