/**
 *
 */
package se.redfield.knime.neo4j.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import junit.framework.AssertionFailedError;
import se.redfield.knime.neo4j.utils.KNimeHelper;
import se.redfield.knime.neo4j.utils.Neo4jHelper;
import se.redfield.knime.runner.KnimeTestRunner;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@RunWith(KnimeTestRunner.class)
public class ConnectorModelTest {
    private ConnectorModel model;
    private ConnectorConfig config;

    /**
     * Default constructor.
     */
    public ConnectorModelTest() {
        super();
    }

    @Before
    public void setUp() {
        model = new ConnectorModel();
        config = Neo4jHelper.createConfig();
    }

    @Test
    public void testConfigure() throws InvalidSettingsException {
        setConfigToModel(config);
        final PortObjectSpec[] result = model.configure(new PortObjectSpec[0]);
        assertEquals(1, result.length);
        assertTrue(result[0] instanceof ConnectorSpec);
    }
    @Test
    public void testConfigureBadConfig() throws URISyntaxException, InvalidSettingsException {
        config.setLocation(new URI("bolt://notexistingsite:4444"));
        setConfigToModel(config);

        //should not throw exception
        final PortObjectSpec[] result = model.configure(new PortObjectSpec[0]);
        assertEquals(1, result.length);
        assertTrue(result[0] instanceof ConnectorSpec);
    }
    @Test
    public void testExecute() throws Exception {
        setConfigToModel(config);
        final PortObject[] result = model.execute(new PortObject[0],
                KNimeHelper.createExecutionContext(model));
        assertEquals(1, result.length);
        assertTrue(result[0] instanceof ConnectorPortObject);
    }
    @Test
    public void testExecuteBadConfig() throws InvalidSettingsException, URISyntaxException {
        config.setLocation(new URI("bolt://notexistingsite:4444"));
        setConfigToModel(config);
        try {
            model.execute(new PortObject[0],
                    KNimeHelper.createExecutionContext(model));
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            //OK
        }
    }
    private void setConfigToModel(final ConnectorConfig cfg) throws InvalidSettingsException {
        final NodeSettings s = new NodeSettings("junit");
        new ConnectorConfigSerializer().save(cfg, s);

        model.loadValidatedSettingsFrom(s);
    }
}
