/**
 *
 */
package se.redfield.knime.table.runner;

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
public class Neo4JTestContext {
    private static Neo4JTestContext current;
    private Driver driver;

    Neo4JTestContext(final String url, final String login, final String password) {
        super();
        //create config
        final ConnectorConfig cfg = new ConnectorConfig();
        try {
            cfg.setLocation(new URI(url));
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
        cfg.getAuth().setScheme(AuthScheme.basic);
        cfg.getAuth().setPrincipal(login);
        cfg.getAuth().setCredentials(password);

        //create support

        //create driver
        final Neo4jSupport s = new Neo4jSupport(cfg);
        this.driver = s.createDriver();
    }

    public Driver getDriver() {
        return driver;
    }

    public static Neo4JTestContext getCurrent() {
        return current;
    }
    static void setCurrent(final Neo4JTestContext current) {
        Neo4JTestContext.current = current;
    }

    public static void destroy() {
        final Driver d = current.driver;
        current = null;
        d.close();
    }
}
