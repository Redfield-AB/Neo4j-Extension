/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.neo4j.driver.Driver;

import se.redfield.knime.neo4jextension.cfg.ConnectorConfig;
import se.redfield.knime.neo4jextension.cfg.ConnectorConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JConnectorModel extends NodeModel {
    private ConnectorConfig connector;

    /**
     * Default constructor.
     */
    public Neo4JConnectorModel() {
        super(new PortType[0], new PortType[] {ConnectorPortObject.TYPE});
        this.connector = new ConnectorConfig();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        new ConnectorConfigSerializer().save(connector, settings);
    }
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        connector = new ConnectorConfigSerializer().load(settings);
    }
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //attempt to load settings.
        new ConnectorConfigSerializer().load(settings);
    }
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {}
    @Override
    protected void reset() {
        connector.reset();
    }
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return configure();
    }
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[0];
    }
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final PortObjectSpec[] spec = configure();

        final ConnectorSpec s = (ConnectorSpec) spec[0];
        testConnection(s.getConnector());

        return new PortObject[]{new ConnectorPortObject(s)};
    }

    /**
     * @param c connector to test.
     */
    private void testConnection(final ConnectorConfig c) throws InvalidSettingsException {
        final Driver driver = ConnectorPortObject.createDriver(c);
        try {
            driver.verifyConnectivity();
        } finally {
            driver.close();
        }
    }
    private PortObjectSpec[] configure() {
        return new PortObjectSpec[] {new ConnectorSpec(connector)};
    }
}
