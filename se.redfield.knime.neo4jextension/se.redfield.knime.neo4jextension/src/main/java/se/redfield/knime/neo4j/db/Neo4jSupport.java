/**
 *
 */
package se.redfield.knime.neo4j.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.QueryType;
import org.neo4j.driver.summary.ResultSummary;

import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.NamedWithProperties;
import se.redfield.knime.neo4j.connector.cfg.AdvancedSettings;
import se.redfield.knime.neo4j.connector.cfg.AuthConfig;
import se.redfield.knime.neo4j.connector.cfg.ConnectorConfig;
import se.redfield.knime.neo4j.connector.cfg.SslTrustStrategy;
import se.redfield.knime.neo4j.utils.ThreadPool;

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
    public static List<Record> runRead(final Driver driver, final String query, final RollbackListener l) {
        return runWithSession(driver, s ->  {
            return s.readTransaction(tx -> {
                final Result run = tx.run(query);
                final List<Record> list = run.list();

                final ResultSummary summary = run.consume();
                if (summary.queryType() != QueryType.READ_ONLY) {
                    tx.rollback();
                    if (l != null) {
                        l.isRolledBack(summary.notifications());
                    }
                }
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
    public static <R> R runWithSession(final Driver driver, final WithSessionRunnable<R> r) {
        final Session s = driver.session();
        try {
            return r.run(s);
        } finally {
            s.close();
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
        final AuthConfig auth = con.getAuth();
        final AuthToken token = auth == null ? null :  AuthTokens.basic(
                auth.getPrincipal(), auth.getCredentials(), null);

        final Driver d = GraphDatabase.driver(con.getLocation(), token,
                createConfig(con.getAdvancedSettings()));
        try {
            d.verifyConnectivity();
        } catch (final RuntimeException e) {
            d.close();
            throw e;
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
    public LabelsAndFunctions loadLabesAndFunctions() throws Exception {
        final LabelsAndFunctions data = new LabelsAndFunctions();

        final List<WithSessionAsyncRunnable<Void>> runs = new ArrayList<>(3);

        final Map<String, NamedWithProperties> nodes = new HashMap<>();
        final Map<String, NamedWithProperties> relationships = new HashMap<>();
        final List<FunctionDesc> functions = new LinkedList<>();

        runs.add(s -> loadNamedWithProperties(s, "call db.labels()", nodes));
        runs.add(s -> loadNodeLabelPropertiess(s, nodes));
        runs.add(s -> loadNamedWithProperties(s, "call db.relationshipTypes()", relationships));
        runs.add(s -> loadRelationshipProperties(s, relationships));
        runs.add(s -> loadFunctions(s, functions));

        runAndWait(runs);

        data.getNodes().addAll(new LinkedList<NamedWithProperties>(nodes.values()));
        data.getRelationships().addAll(new LinkedList<NamedWithProperties>(relationships.values()));
        data.getFunctions().addAll(functions);

        return data;
    }

    private void loadNamedWithProperties(final Session s, final String query,
            final Map<String, NamedWithProperties> map) {
        final List<Record> result = s.readTransaction(tx -> tx.run(query).list());
        for (final Record r : result) {
            final String type = r.get(0).asString();
            synchronized (map) {
                if (!map.containsKey(type)) {
                    map.put(type, new NamedWithProperties(type));
                }
            }
        }
    }
    private void loadNodeLabelPropertiess(final Session s, final Map<String, NamedWithProperties> map) {
        final List<Record> result = s.readTransaction(tx -> tx.run(
                "call db.schema.nodeTypeProperties()").list());
        for (final Record r : result) {
            final String property = r.get("propertyName").asString();
            final List<Object> nodeLabels = r.get("nodeLabels").asList();

            for (final Object obj : nodeLabels) {
                final String type = (String) obj;

                NamedWithProperties n;
                synchronized (map) {
                    n = map.get(type);
                    if (n == null) {
                        n = new NamedWithProperties(type);
                        map.put(type, n);
                    }
                }

                if (property != null && !property.equals("null")) {
                    n.getProperties().add(property);
                }
            }
        }
    }

    private void loadRelationshipProperties(final Session s, final Map<String, NamedWithProperties> map) {
        final List<Record> result = s.readTransaction(tx -> tx.run(
                "call db.schema.relTypeProperties()").list());
        for (final Record r : result) {
            final String property = r.get("propertyName").asString();
            String type = r.get("relType").asString();
            if (type.startsWith(":")) {
                type = type.substring(2, type.length() - 1);
            }

            NamedWithProperties n;
            synchronized (map) {
                n = map.get(type);
                if (n == null) {
                    n = new NamedWithProperties(type);
                    map.put(type, n);
                }
            }

            if (property != null && !property.equals("null")) {
                n.getProperties().add(property);
            }
        }
    }
    private void loadFunctions(final Session s, final List<FunctionDesc> functions) {
        final List<Record> result = s.readTransaction(tx -> tx.run(
                "call dbms.functions()").list());
        for (final Record r : result) {
            final FunctionDesc f = new FunctionDesc();
            f.setName(r.get("name").asString());
            f.setSignature(r.get("signature").asString());
            f.setDescription(r.get("description").asString());
            functions.add(f);
        }
    }
    public ConnectorConfig getConfig() {
        return config;
    }
}
