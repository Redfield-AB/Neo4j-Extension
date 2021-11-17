/**
 *
 */
package se.redfield.knime.neo4j.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.knime.core.node.ExecutionContext;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Config.ConfigBuilder;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.summary.QueryType;
import org.neo4j.driver.summary.ResultSummary;

import se.redfield.knime.neo4j.async.AsyncRunnerLauncher;
import se.redfield.knime.neo4j.connector.AuthConfig;
import se.redfield.knime.neo4j.connector.ConnectorConfig;
import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.NamedWithProperties;

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
    public static List<Record> runRead(final Driver driver, final String query, final RollbackListener l, final String dataBase) {
        return runWithSession(driver, s ->  runInReadOnlyTransaction(s, query, l), dataBase);
    }
    /**
     * @param session session.
     * @param query query.
     * @param listener rollback listener.
     * @return execution result.
     */
    public static List<Record> runInReadOnlyTransaction(final Session session, final String query,
            final RollbackListener listener) {
        final Transaction tx = session.beginTransaction();
        try {
            final Result run = tx.run(query);
            final List<Record> list = run.list();

            final ResultSummary summary = run.consume();
            if (summary.queryType() != QueryType.READ_ONLY) {
                tx.rollback();
                if (listener != null) {
                    listener.isRolledBack();
                }
            }
            return list;
        } finally {
            tx.close();
        }
    }
    public static <R> R runWithSession(final Driver driver, final WithSessionRunnable<R> r, final String dataBase) {
        final Session s = driver.session(SessionConfig.forDatabase(dataBase));
        try {
            return r.run(s);
        } finally {
            s.close();
        }
    }
    public Driver createDriver() {
        return createDriver(config);
    }
    public ContextListeningDriver createDriver(final ExecutionContext context) {
        final Driver d = createDriver(config);
        return new ContextListeningDriver(d, context);
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
                createConfig(con.getMaxConnectionPoolSize()));
        try {
            d.verifyConnectivity();
        } catch (final RuntimeException e) {
            d.close();
            throw e;
        }
        return d;
    }
    private static Config createConfig(final int poolSize) {
        final ConfigBuilder cfg = Config.builder();
        cfg.withMaxConnectionPoolSize(poolSize);
        return cfg.build();
    }
    public LabelsAndFunctions loadLabesAndFunctions() throws Exception {
        final Map<String, NamedWithProperties> nodes = new HashMap<>();
        final Map<String, NamedWithProperties> relationships = new HashMap<>();
        final List<FunctionDesc> functions = new LinkedList<>();

        final Driver driver = createDriver();
        try {
            final List<WithSessionRunnable<Void>> runs = new ArrayList<>(3);
            runs.add(s -> loadNamedWithProperties(s, "call db.labels()", nodes));
            runs.add(s -> loadNodeLabelPropertiess(s, nodes));
            runs.add(s -> loadNamedWithProperties(s, "call db.relationshipTypes()", relationships));
            runs.add(s -> loadRelationshipProperties(s, relationships));
            runs.add(s -> loadFunctions(s, functions));

            final AsyncRunnerLauncher<WithSessionRunnable<Void>, Void> runner
                = AsyncRunnerLauncher.Builder.<WithSessionRunnable<Void>, Void>newBuilder()
                    .withRunner((r) -> runWithSession(driver, r, config.getDatabase()))
                    .withSource(runs.iterator())
                    .withNumThreads(runs.size())
                    .withStopOnFailure(true)
                    .build();
            runner.run();

            if (runner.hasErrors()) {
                throw new Exception("Failed to read Neo4j DB metadata");
            }
        } finally {
            driver.closeAsync();
        }

        final LabelsAndFunctions data = new LabelsAndFunctions();
        data.getNodes().addAll(new LinkedList<NamedWithProperties>(nodes.values()));
        data.getRelationships().addAll(new LinkedList<NamedWithProperties>(relationships.values()));
        data.getFunctions().addAll(functions);

        return data;
    }

    private Void loadNamedWithProperties(final Session s, final String query,
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
        return null;
    }
    private Void loadNodeLabelPropertiess(final Session s, final Map<String, NamedWithProperties> map) {
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
        return null;
    }

    private Void loadRelationshipProperties(final Session s, final Map<String, NamedWithProperties> map) {
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
        return null;
    }
    private Void loadFunctions(final Session s, final List<FunctionDesc> functions) {
        final List<Record> result = s.readTransaction(tx -> tx.run(
                "call dbms.functions()").list());
        for (final Record r : result) {
            final FunctionDesc f = new FunctionDesc();
            f.setName(r.get("name").asString());
            f.setSignature(r.get("signature").asString());
            f.setDescription(r.get("description").asString());
            functions.add(f);
        }
        return null;
    }
    public ConnectorConfig getConfig() {
        return config;
    }
}
