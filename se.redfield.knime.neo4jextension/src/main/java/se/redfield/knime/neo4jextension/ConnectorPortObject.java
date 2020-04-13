/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
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
import org.neo4j.driver.types.TypeSystem;

import se.redfield.knime.neo4jextension.cfg.AdvancedSettings;
import se.redfield.knime.neo4jextension.cfg.AuthConfig;
import se.redfield.knime.neo4jextension.cfg.Neo4JConfig;
import se.redfield.knime.neo4jextension.cfg.SslTrustStrategy;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorPortObject extends AbstractSimplePortObject {
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(
            ConnectorPortObject.class);
    public static final PortType TYPE_OPTIONAL = PortTypeRegistry.getInstance().getPortType(
            ConnectorPortObject.class, true);

    private final ConnectorSpec spec;
    private Driver driver;

    public ConnectorPortObject() {
        this(new ConnectorSpec());
    }
    public ConnectorPortObject(final ConnectorSpec connector) {
        super();
        this.spec = connector;
    }

    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
            throws InvalidSettingsException, CanceledExecutionException {
        this.spec.load(model);
    }
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        this.spec.save(model);
    }
    @Override
    public String getSummary() {
        final StringBuilder sb = new StringBuilder("NeoJ4 DB: ");
        sb.append(spec.getConnector().getLocation());
        return sb.toString();
    }
    @Override
    public ConnectorSpec getSpec() {
        return spec;
    }
    public List<Record> run(final String query) {
        final Driver driver = getDriver();
        final Session s = driver.session();
        try {
            return s.readTransaction( tx -> tx.run(query).list());
        } finally {
            s.close();
        }
    }
    /**
     * @return
     */
    private Driver getDriver() {
        if (this.driver == null) {
            this.driver = createDriver(spec.getConnector());
        }
        return driver;
    }
    /**
     * @param con Neo4J configuration.
     * @return Neo4J driver.
     */
    static Driver createDriver(final Neo4JConfig con) {
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
        return getDriver().defaultTypeSystem();
    }
}
