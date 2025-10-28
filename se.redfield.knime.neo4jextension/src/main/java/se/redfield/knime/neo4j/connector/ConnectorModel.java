/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.oauth.api.AccessTokenAccessor;
import org.knime.credentials.base.oauth.api.JWTCredential;
import org.knime.credentials.base.NoSuchCredentialException;

import se.redfield.knime.neo4j.db.Neo4jSupport;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorModel extends NodeModel {
    private ConnectorConfig config;
    private final int m_credentialPortIdx;
    
    
    public ConnectorModel(final NodeCreationConfiguration cfg) {
        //super(new PortType[0], new PortType[] {ConnectorPortObject.TYPE});
        super(cfg.getPortConfig().orElseThrow(IllegalStateException::new).getInputPorts(),
                cfg.getPortConfig().orElseThrow(IllegalStateException::new).getOutputPorts());
            // the field m_credentialIdx tracks the index of the credential port if present.
            
        m_credentialPortIdx = cfg.getPortConfig().orElseThrow(IllegalStateException::new).getInputPortLocation()
                .getOrDefault(ConnectorFactory.CREDENTIAL_GROUP_ID, new int[]{-1})[0];    	
        this.config = new ConnectorConfig();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        new ConnectorConfigSerializer().save(config, settings);
    }
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        config = new ConnectorConfigSerializer().load(settings);
    }
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //attempt to load settings.
        new ConnectorConfigSerializer().load(settings);
    }
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {}
    @Override
    protected void reset() {
    }
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_credentialPortIdx >= 0 && inSpecs[m_credentialPortIdx] != null) {
            var credSpec = (CredentialPortObjectSpec) inSpecs[m_credentialPortIdx];
            var credType = credSpec.getCredentialType().orElse(null);
            
            if (credType != null && !JWTCredential.class.isAssignableFrom(credType.getCredentialClass())) {
                setWarningMessage("Connected credential is not a JWT credential. OAuth2 authentication may fail.");
            }
        }
        
        return configure();
    }
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[0];
    }
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final ConnectorConfig resolvedConfig = config.createResolvedConfig(getCredentialsProvider());

        // Try to get OAuth2 token from credential port
        if (m_credentialPortIdx != -1 && inObjects[m_credentialPortIdx] instanceof CredentialPortObject) {
            CredentialPortObject credPortObj = (CredentialPortObject) inObjects[m_credentialPortIdx];
            try {
                JWTCredential jwtCredential = credPortObj.getSpec().resolveCredential(JWTCredential.class);
                if (jwtCredential != null) {
                    String oauthToken = jwtCredential.getAccessToken();
                    if ((oauthToken == null || oauthToken.isEmpty()) && jwtCredential instanceof AccessTokenAccessor) {
                        getLogger().info("Token is null or empty, attempting to refresh...");
                        oauthToken = ((AccessTokenAccessor) jwtCredential).getAccessToken(true);
                    }
                    resolvedConfig.setOauthToken(oauthToken);
                    resolvedConfig.getAuth().setScheme(AuthScheme.OAuth2);
                }
            } catch (NoSuchCredentialException | IOException ex) {
                throw new Exception("Failed to retrieve JWT credential or refresh token: " + ex.getMessage(), ex);
            }
        }

        // Test connection using the resolved config
        final Neo4jSupport s = new Neo4jSupport(resolvedConfig);
        try (var driver = s.createDriver(); var session = driver.session()) {
            session.run("RETURN 1");
        } catch (Exception e) {
            throw new Exception("Failed to connect to Neo4j: " + e.getMessage(), e);
        }

        return new PortObject[]{new ConnectorPortObject(resolvedConfig)};
    }

    private PortObjectSpec[] configure() {
        return new PortObjectSpec[] {new ConnectorSpec(config)};
    }
}
