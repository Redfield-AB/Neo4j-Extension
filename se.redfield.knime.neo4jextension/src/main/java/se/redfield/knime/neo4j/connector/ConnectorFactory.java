/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.util.Optional;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.credentials.base.CredentialPortObject;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorFactory extends ConfigurableNodeFactory<ConnectorModel> {
    /**
     * Default constructor.
     */
    public ConnectorFactory() {
        super();
    }

    static final String CREDENTIAL_GROUP_ID = "Credential";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        var builder = new PortsConfigurationBuilder();

        builder.addOptionalInputPortGroup(CREDENTIAL_GROUP_ID, CredentialPortObject.TYPE);
        builder.addFixedOutputPortGroup("Connection", ConnectorPortObject.TYPE);

        return Optional.of(builder);
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
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return new ConnectorDialog();
    }

    @Override
    protected ConnectorModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new ConnectorModel(creationConfig);
    }
}
