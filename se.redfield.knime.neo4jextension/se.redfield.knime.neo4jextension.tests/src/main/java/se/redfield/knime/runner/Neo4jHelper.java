/**
 *
 */
package se.redfield.knime.runner;

import java.net.URI;
import java.net.URISyntaxException;

import org.neo4j.driver.Driver;

import se.redfield.knime.neo4j.connector.cfg.AuthScheme;
import se.redfield.knime.neo4j.connector.cfg.ConnectorConfig;
import se.redfield.knime.neo4j.db.Neo4jSupport;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public final class Neo4jHelper {
    private static final String DBUSER = "neo4j";
    private static final String PASSWORD = "password";
    private static final String URL = "bolt://localhost:7687";

    private Neo4jHelper() {
        super();
    }

    public static Driver createDriver() {
        final ConnectorConfig cfg = new ConnectorConfig();
        try {
            cfg.setLocation(new URI(URL));
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
        cfg.getAuth().setScheme(AuthScheme.basic);
        cfg.getAuth().setPrincipal(DBUSER);
        cfg.getAuth().setCredentials(PASSWORD);

        //create support

        //create driver
        final Neo4jSupport s = new Neo4jSupport(cfg);
        return s.createDriver();
    }
}
