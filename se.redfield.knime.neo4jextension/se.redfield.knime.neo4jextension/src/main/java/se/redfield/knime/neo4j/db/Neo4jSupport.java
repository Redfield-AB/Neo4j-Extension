/**
 *
 */
package se.redfield.knime.neo4j.db;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.knime.core.node.InvalidSettingsException;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Config.ConfigBuilder;
import org.neo4j.driver.Config.TrustStrategy;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.TypeSystem;

import se.redfield.knime.neo4j.connector.cfg.AdvancedSettings;
import se.redfield.knime.neo4j.connector.cfg.AuthConfig;
import se.redfield.knime.neo4j.connector.cfg.ConnectorConfig;
import se.redfield.knime.neo4j.reader.cfg.SslTrustStrategy;
import se.redfield.knime.utils.ThreadPool;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4jSupport {
    private final ConnectorConfig config;

    public Neo4jSupport(final ConnectorConfig config) {
        super();
        this.config = config;
    }
    public List<Record> runRead(final String query) {
        return runWithSession(s ->  {
            return s.readTransaction(tx -> {
                final List<Record> list = tx.run(query).list();
                tx.rollback();
                return list;
            });
        });
    }
    public <R> R runWithDriver(final WithDriverRunnable<R> r) {
        final Driver driver = createDriver();
        try {
            return r.run(driver);
        } finally {
            driver.close();
        }
    }
    public <R> R runWithSession(final WithSessionRunnable<R> r) {
        final Driver driver = createDriver();
        try {
            final Session s = driver.session();
            try {
                return r.run(s);
            } finally {
                s.close();
            }
        } finally {
            driver.close();
        }
    }
    public void runAndWait(final List<WithSessionAsyncRunnable<Void>> runs) throws Exception {
        final AbstractExecutorService executor = ThreadPool.getExecutor();
        final Driver driver = createDriver();

        final List<Exception> errors = new LinkedList<>();
        final List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(runs.size());
        for (final WithSessionAsyncRunnable<Void> r : runs) {
            //create callables
            final Callable<Void> c = () -> {
                final Session s = driver.session();
                try {
                    r.run(s);
                } catch (final Exception e) {
                    errors.add(e);
                } finally {
                    s.close();
                }
                return null;
            };

            tasks.add(c);
        }

        try {
            try {
                executor.invokeAll(tasks);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            driver.close();
        }

        if (!errors.isEmpty()) {
            final Exception exc = errors.remove(0);
            for (final Exception e : errors) {
                exc.addSuppressed(e);
            }

            throw exc;
        }
    }
    public void runAsync(final WithSessionRunnable<Void> run) {
        final AbstractExecutorService executor = ThreadPool.getExecutor();
        final Driver driver = createDriver();

        //create callables
        final Callable<Void> c = () -> {
            final Session s = driver.session();
            try {
                run.run(s);
            } finally {
                s.close();
                driver.close();
            }
            return null;
        };

        executor.submit(c);
    }
    public Driver createDriver() {
        return createDriver(config);
    }
    /**
     * @param con Neo4J configuration.
     * @return Neo4J driver.
     */
    private static Driver createDriver(final ConnectorConfig con) {
        final URI location = con.getLocation();
        //final Config config = connector.getConnector().getConfig();
        final AuthConfig auth = con.getAuth();

        Driver d;
        if (auth == null) {
            d = GraphDatabase.driver(location);
        } else {
            final AuthToken token = AuthTokens.basic(
                    auth.getPrincipal(), auth.getCredentials(), null);
            d = GraphDatabase.driver(location, token, createConfig(con.getAdvancedSettings()));
        }
        return d;
    }
    private static Config createConfig(final AdvancedSettings as) {
        final ConfigBuilder cfg = Config.builder();

        cfg.withConnectionAcquisitionTimeout(as.getConnectionAcquisitionTimeoutMillis(), TimeUnit.MICROSECONDS);
        cfg.withConnectionTimeout(as.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
        if (as.isEncrypted()) {
            cfg.withEncryption();
        } else {
            cfg.withoutEncryption();
        }
        cfg.withEventLoopThreads(as.getEventLoopThreads());
        cfg.withFetchSize(as.getFetchSize());
        cfg.withConnectionLivenessCheckTimeout(
                as.getIdleTimeBeforeConnectionTest(), TimeUnit.MILLISECONDS);
        if (as.isMetricsEnabled()) {
            cfg.withDriverMetrics();
        } else {
            cfg.withoutDriverMetrics();
        }
        if (as.isLogLeakedSessions()) {
            cfg.withLeakedSessionsLogging();
        }
        cfg.withMaxConnectionLifetime(as.getMaxConnectionLifetimeMillis(),
                TimeUnit.MILLISECONDS);
        cfg.withMaxConnectionPoolSize(as.getMaxConnectionPoolSize());

        if (as.getTrustStrategy() != null) {
            cfg.withTrustStrategy(loadTrustStrategy(as.getTrustStrategy()));
        }

        return cfg.build();
    }
    /**
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static TrustStrategy loadTrustStrategy(final SslTrustStrategy settings) {
        final TrustStrategy.Strategy strategy = settings.getStrategy();

        TrustStrategy s;
        switch (strategy) {
            case TRUST_ALL_CERTIFICATES:
                s = TrustStrategy.trustAllCertificates();
                break;
            case TRUST_SYSTEM_CA_SIGNED_CERTIFICATES:
                s = TrustStrategy.trustSystemCertificates();
                break;
            case  TRUST_CUSTOM_CA_SIGNED_CERTIFICATES:
            default:
                s = TrustStrategy.trustCustomCertificateSignedBy(settings.getCertFile());
                break;
        }
        return s;
    }
    public DataAdapter createDataAdapter() {
        final TypeSystem ts = runWithDriver(d -> d.defaultTypeSystem());
        return new DataAdapter(ts);
    }
}
