/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Config.ConfigBuilder;
import org.neo4j.driver.Config.TrustStrategy;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;

import se.redfield.knime.neo4jextension.cfg.AdvancedSettings;
import se.redfield.knime.neo4jextension.cfg.AuthConfig;
import se.redfield.knime.neo4jextension.cfg.ConnectorConfig;
import se.redfield.knime.neo4jextension.cfg.ConnectorConfigSerializer;
import se.redfield.knime.neo4jextension.cfg.SslTrustStrategy;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorPortObject extends AbstractSimplePortObject {
    private static final String RELATIONSHIP_TYPES_KEY = "relationshipTypes";
    private static final String NODE_LABELS_KEY = "nodeLabels";

    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(
            ConnectorPortObject.class);
    public static final PortType TYPE_OPTIONAL = PortTypeRegistry.getInstance().getPortType(
            ConnectorPortObject.class, true);

    private ConnectorConfig config;

    private List<String> nodeLabels = new LinkedList<>();
    private List<String> relationshipTypes = new LinkedList<>();

    public ConnectorPortObject() {
        this(new ConnectorConfig());
    }
    public ConnectorPortObject(final ConnectorConfig connector) {
        super();
        this.config = connector;
    }

    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
            throws InvalidSettingsException, CanceledExecutionException {
        load(model);
    }
    public void load(final ConfigRO model) throws InvalidSettingsException {
        config = new ConnectorConfigSerializer().load(model);

        //load node labels
        if (model.containsKey(NODE_LABELS_KEY)) {//check contains key for backward compatibility
            for (final String label : model.getStringArray(NODE_LABELS_KEY)) {
                this.nodeLabels.add(label);
            }
        }
        //load relationship types
        if (model.containsKey(RELATIONSHIP_TYPES_KEY)) {
            for (final String type : model.getStringArray(RELATIONSHIP_TYPES_KEY)) {
                this.relationshipTypes.add(type);
            }
        }
    }
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        save(model);
    }
    public void save(final ConfigWO model) {
        new ConnectorConfigSerializer().save(config, model);

        //save node labels
        model.addStringArray(NODE_LABELS_KEY,
                nodeLabels.toArray(new String[nodeLabels.size()]));
        model.addStringArray(RELATIONSHIP_TYPES_KEY,
                relationshipTypes.toArray(new String[relationshipTypes.size()]));
    }
    @Override
    public String getSummary() {
        final StringBuilder sb = new StringBuilder("NeoJ4 DB: ");
        sb.append(config.getLocation());
        return sb.toString();
    }
    @Override
    public ConnectorSpec getSpec() {
        return new ConnectorSpec();
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
    public ResultSummary runUpdate(final String query) {
        return runWithSession(s -> s.readTransaction(tx -> tx.run(query).consume()));
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
    private Driver createDriver() {
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
    public TypeSystem getTypeSystem() {
        return runWithDriver(d -> d.defaultTypeSystem());
    }
    /**
     * @param labels node labels.
     */
    public void setNodeLabels(final List<String> labels) {
        this.nodeLabels = labels;
    }
    /**
     * @return node labels.
     */
    public List<String> getNodeLabels() {
        return nodeLabels;
    }
    /**
     * @param labels relationship labels.
     */
    public void setRelationshipTypes(final List<String> labels) {
        this.relationshipTypes = labels;
    }
    /**
     * @return relationship labels.
     */
    public List<String> getRelationshipTypes() {
        return relationshipTypes;
    }
    public ConnectorConfig getConnector() {
        return this.config;
    }
}
