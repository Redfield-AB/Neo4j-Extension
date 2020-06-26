/**
 *
 */
package se.redfield.knime.neo4j.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;

import se.redfield.knime.runner.KnimeTestRunner;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@RunWith(KnimeTestRunner.class)
public class ConnectorConfigSerializerTest {
    /**
     * Default constructor.
     */
    public ConnectorConfigSerializerTest() {
        super();
    }

    @Test
    public void testSerialize() throws URISyntaxException, InvalidSettingsException {
        final AuthConfig auth = new AuthConfig();
        auth.setCredentials("password");
        auth.setPrincipal("junit");
        auth.setScheme(AuthScheme.flowCredentials);

        final URI location = new URI("bolt://host:777");
        final int maxConnectionPoolSize = 17;

        ConnectorConfig cfg = new ConnectorConfig();
        cfg.setAuth(auth);
        cfg.setLocation(location);
        cfg.setMaxConnectionPoolSize(maxConnectionPoolSize);

        final NodeSettings s = new NodeSettings("junit");
        final ConnectorConfigSerializer ser = new ConnectorConfigSerializer();
        ser.save(cfg, s);
        cfg = ser.load(s);

        //test result
        assertEquals(location, cfg.getLocation());
        assertEquals(maxConnectionPoolSize, cfg.getMaxConnectionPoolSize());
        assertEquals(auth, cfg.getAuth());
        assertEquals(auth.getCredentials(), cfg.getAuth().getCredentials());
        assertEquals(auth.getPrincipal(), cfg.getAuth().getPrincipal());
        assertEquals(auth.getScheme(), cfg.getAuth().getScheme());
    }
    @Test
    public void testSerializeNullAuth() throws InvalidSettingsException {
        ConnectorConfig cfg = new ConnectorConfig();
        cfg.setAuth(null);

        final NodeSettings s = new NodeSettings("junit");
        final ConnectorConfigSerializer ser = new ConnectorConfigSerializer();
        ser.save(cfg, s);
        cfg = ser.load(s);

        //test result
        assertNull(cfg.getAuth());
    }
}
