/**
 *
 */
package se.redfield.knime.neo4j.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import se.redfield.knime.neo4j.connector.AuthScheme;
import se.redfield.knime.neo4j.connector.ConnectorConfig;
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
        final Neo4jSupport s = createSupport();
        return s.createDriver();
    }
    public static Neo4jSupport createSupport() {
        final ConnectorConfig cfg = createConfig();
        return new Neo4jSupport(cfg);
    }

    /**
     * @return connector configuration.
     */
    public static ConnectorConfig createConfig() {
        final ConnectorConfig cfg = new ConnectorConfig();
        try {
            cfg.setLocation(new URI(URL));
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
        cfg.getAuth().setScheme(AuthScheme.basic);
        cfg.getAuth().setPrincipal(DBUSER);
        cfg.getAuth().setCredentials(PASSWORD);
        return cfg;
    }

    /**
     * @param query query to run.
     */
    public static void write(final String query) {
        final Driver d = Neo4jHelper.createDriver();
        try {
            final Session s = d.session();
            s.writeTransaction(t -> {
                t.run(query);
                t.commit();
                return null;
            });
        } finally {
            d.close();
        }
    }
    /**
     * @param query query to run.
     * @return list of records.
     */
    public static List<Record> read(final String query) {
        final Driver d = Neo4jHelper.createDriver();
        try {
            final Session s = d.session();
            return s.readTransaction(t -> {
                final Result res = t.run(query);
                return res.list();
            });
        } finally {
            d.close();
        }
    }
}
