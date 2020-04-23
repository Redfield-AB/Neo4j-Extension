/**
 *
 */
package se.redfield.knime.neo4jextension;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

import se.redfield.knime.neo4jextension.cfg.ConnectorPortData;
import se.redfield.knime.neo4jextension.cfg.ConnectorPortDataSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorPortObject extends AbstractSimplePortObject {
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(
            ConnectorPortObject.class);
    public static final PortType TYPE_OPTIONAL = PortTypeRegistry.getInstance().getPortType(
            ConnectorPortObject.class, true);

    private ConnectorPortData data;

    public ConnectorPortObject() {
        this(new ConnectorPortData());
    }
    public ConnectorPortObject(final ConnectorPortData data) {
        super();
        this.data = data;
    }

    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
            throws InvalidSettingsException, CanceledExecutionException {
        data = new ConnectorPortDataSerializer().load(model);
    }
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        new ConnectorPortDataSerializer().save(data, model);
    }
    @Override
    public String getSummary() {
        final StringBuilder sb = new StringBuilder("NeoJ4 DB: ");
        sb.append(data.getConnectorConfig().getLocation());
        return sb.toString();
    }
    @Override
    public ConnectorSpec getSpec() {
        return new ConnectorSpec();
    }
    public ConnectorPortData getPortData() {
        return data;
    }
}
