/**
 *
 */
package se.redfield.knime.neo4j.connector;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentAuthentication;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.credentials.base.CredentialPortObjectSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorDialog extends NodeDialogPane {

    public static final String DEFAULT_DATABASE_NAME = "neo4j";

    //connection
    private final JTextField url = new JTextField();
    private final JFormattedTextField maxConnectionPoolSize = createIntValueEditor();
    private final JRadioButton defaultRBtn = new JRadioButton("default");
    private final JRadioButton customRBtn = new JRadioButton("custom");
    private boolean usedDefaultDbName = true;
    private final JTextField database = new JTextField(DEFAULT_DATABASE_NAME);
    private String customDBNameBuffer = DEFAULT_DATABASE_NAME;

    private final SettingsModelAuthentication authSettings = new SettingsModelAuthentication(
            "neo4jAuth", AuthenticationType.USER_PWD, "neo4j", null, null);

    // Custom authentication UI components
    private final JRadioButton noneAuthRBtn = new JRadioButton("None");
    private final JRadioButton userPwdAuthRBtn = new JRadioButton("User/Password");
    private final JRadioButton credentialsAuthRBtn = new JRadioButton("Credentials");
    private final JRadioButton oauth2AuthRBtn = new JRadioButton("OAuth2");
    private final ButtonGroup authButtonGroup = new ButtonGroup();
    private final JPanel authCards = new JPanel(new CardLayout());

    // Panels for each authentication type
    private final JPanel noneAuthPanel = new JPanel(); // Empty panel for None
    private final JPanel oauth2AuthPanel = new JPanel(); // Empty panel for OAuth2
    private final DialogComponentAuthentication userPwdAuthComp = new DialogComponentAuthentication(
            authSettings, "User/Password Authentication", AuthenticationType.USER_PWD);
    private final DialogComponentAuthentication credentialsAuthComp = new DialogComponentAuthentication(
            authSettings, "Credentials Authentication", AuthenticationType.CREDENTIALS);

    public static final String OAUTH2_CREDENTIAL_IDENTIFIER = "OAuth2_Credential_From_Port";

    private boolean m_hasCredentialPort; // To track if the credential port is connected

    /**
     * Default constructor.
     */
    public ConnectorDialog() {
        super();
        addTab("Connection", createConnectionPage());
    }

    private JFormattedTextField createIntValueEditor() {
        final JFormattedTextField tf = new JFormattedTextField(NumberFormat.getIntegerInstance());
        tf.setValue(0);
        return tf;
    }

    /**
     * @return
     */
    private Component createConnectionPage() {
        final JPanel p = new JPanel(new BorderLayout(10, 5));
        p.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Connection
        // URL
        final JPanel north = new JPanel(new GridBagLayout());
        north.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Connection"));
        p.add(north, BorderLayout.NORTH);

        addLabeledComponent(north, "Neo4j URL", url, 0);
        addLabeledComponent(north, "Max connection pool size:", maxConnectionPoolSize, 1);

        // Set database name
        final JPanel p1 = new JPanel(new BorderLayout(10, 5));
        p1.setBorder(BorderFactory.createTitledBorder(new EmptyBorder(5, 5, 5, 5)));

        defaultRBtn.addActionListener(e -> {
            customDBNameBuffer = database.getText();
            database.setText(DEFAULT_DATABASE_NAME);
            database.setEnabled(false);
            usedDefaultDbName = true;
        });

        customRBtn.addActionListener(e -> {
            database.setText(customDBNameBuffer);
            database.setEnabled(true);
            usedDefaultDbName = false;
        });

        defaultRBtn.setSelected(usedDefaultDbName);
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(defaultRBtn);
        btnGroup.add(customRBtn);

        p1.add(defaultRBtn, BorderLayout.CENTER);
        p1.add(customRBtn, BorderLayout.LINE_START);

        addLabeledComponent(north, "Use database name:", p1, 2);

        addLabeledComponent(north, "Database name:", database, 3);

        // Authentication
        final JPanel authPanel = new JPanel(new BorderLayout(5, 5));
        authPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Authentication"));
        p.add(authPanel, BorderLayout.CENTER);

        final JPanel radioButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authButtonGroup.add(noneAuthRBtn);
        authButtonGroup.add(userPwdAuthRBtn);
        authButtonGroup.add(credentialsAuthRBtn);
        authButtonGroup.add(oauth2AuthRBtn);


        radioButtonsPanel.add(noneAuthRBtn);
        radioButtonsPanel.add(userPwdAuthRBtn);
        radioButtonsPanel.add(credentialsAuthRBtn);
        radioButtonsPanel.add(oauth2AuthRBtn);

        authPanel.add(radioButtonsPanel, BorderLayout.NORTH);
        authPanel.add(authCards, BorderLayout.CENTER);

        // Add cards for each authentication type
        authCards.add(noneAuthPanel, AuthenticationType.NONE.name());
        authCards.add(userPwdAuthComp.getComponentPanel(), AuthenticationType.USER_PWD.name());
        authCards.add(credentialsAuthComp.getComponentPanel(), AuthenticationType.CREDENTIALS.name());
        authCards.add(oauth2AuthPanel, "OAuth2");
        
        // Add action listeners to switch cards
        noneAuthRBtn.addActionListener(e -> {
            authSettings.setValues(AuthenticationType.NONE, null, null, null);
            ((CardLayout) authCards.getLayout()).show(authCards, AuthenticationType.NONE.name());
        });
        userPwdAuthRBtn.addActionListener(e -> {
            authSettings.setValues(AuthenticationType.USER_PWD, null, null, null);
            ((CardLayout) authCards.getLayout()).show(authCards, AuthenticationType.USER_PWD.name());
        });
        credentialsAuthRBtn.addActionListener(e -> {
            authSettings.setValues(AuthenticationType.CREDENTIALS, null, null, null);
            ((CardLayout) authCards.getLayout()).show(authCards, AuthenticationType.CREDENTIALS.name());
        });
        oauth2AuthRBtn.addActionListener(e -> {
            authSettings.setValues(AuthenticationType.CREDENTIALS, OAUTH2_CREDENTIAL_IDENTIFIER, null, null);
            ((CardLayout) authCards.getLayout()).show(authCards, "OAuth2");
        });

        // Default selection
        noneAuthRBtn.setSelected(true);
        ((CardLayout) authCards.getLayout()).show(authCards, AuthenticationType.NONE.name());

        return p;
    }

    /**
     * @param container
     * @param label
     * @param component
     * @param row
     */
    private void addLabeledComponent(final JPanel container, final String label,
                                     final JComponent component, final int row) {
        //add label
        final GridBagConstraints lc = new GridBagConstraints();
        lc.fill = GridBagConstraints.HORIZONTAL;
        lc.gridx = 0;
        lc.gridy = row;
        lc.weightx = 0.;

        final JLabel l = new JLabel(label);
        l.setHorizontalTextPosition(SwingConstants.RIGHT);
        l.setHorizontalAlignment(SwingConstants.RIGHT);

        final JPanel labelWrapper = new JPanel(new BorderLayout());
        labelWrapper.setBorder(new EmptyBorder(0, 0, 0, 5));
        labelWrapper.add(l, BorderLayout.CENTER);
        container.add(labelWrapper, lc);

        //add component.
        final GridBagConstraints cc = new GridBagConstraints();
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.gridx = 1;
        cc.gridy = row;
        cc.weightx = 1.;
        container.add(component, cc);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

        final ConnectorConfig model = buildConnector();
        if (model != null) {
            new ConnectorConfigSerializer().save(model, settings);
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        m_hasCredentialPort = specs != null && specs.length > 0 && specs[0] instanceof CredentialPortObjectSpec;
        
        try {
            final ConnectorConfig model = new ConnectorConfigSerializer().load(settings);
            init(model, specs);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException("Failed to load configuration from settings", e);
        }
    }

    private void init(final ConnectorConfig model, final PortObjectSpec[] specs) throws NotConfigurableException {
        this.url.setText(model.getLocation() == null ? "" : model.getLocation().toASCIIString());
        maxConnectionPoolSize.setValue(model.getMaxConnectionPoolSize());

        database.setText(model.getDatabase() == null ? DEFAULT_DATABASE_NAME : model.getDatabase());
        defaultRBtn.setSelected(model.isUsedDefaultDbName());
        database.setEnabled(!model.isUsedDefaultDbName());

        // Authentication
        final AuthConfig auth = model.getAuth();
        final boolean shouldUseAuth = auth != null;

        // Handle port connection state
        noneAuthRBtn.setEnabled(true);
        userPwdAuthRBtn.setEnabled(true);
        credentialsAuthRBtn.setEnabled(true);
        oauth2AuthRBtn.setEnabled(m_hasCredentialPort);

        if (m_hasCredentialPort) {
            // When credential port is connected, force OAuth2 mode
            authSettings.setValues(AuthenticationType.CREDENTIALS, OAUTH2_CREDENTIAL_IDENTIFIER, null, null);
            oauth2AuthRBtn.setSelected(true);
            ((CardLayout) authCards.getLayout()).show(authCards, "OAuth2");
            
            // Disable other authentication options
            noneAuthRBtn.setEnabled(false);
            userPwdAuthRBtn.setEnabled(false);
            credentialsAuthRBtn.setEnabled(false);
        } else {
            // Port is not connected
            if (shouldUseAuth && auth.getScheme() == AuthScheme.OAuth2) {
                // If the saved config is OAuth2, default to None
                authSettings.setValues(AuthenticationType.NONE, null, null, null);
                noneAuthRBtn.setSelected(true);
                ((CardLayout) authCards.getLayout()).show(authCards, AuthenticationType.NONE.name());
            } else if (shouldUseAuth) {
                // Load the saved settings
                if (auth.getScheme() == AuthScheme.flowCredentials) {
                    authSettings.setValues(AuthenticationType.CREDENTIALS, auth.getPrincipal(), null, null);
                    credentialsAuthRBtn.setSelected(true);
                    ((CardLayout) authCards.getLayout()).show(authCards, AuthenticationType.CREDENTIALS.name());
                } else if (auth.getScheme() == AuthScheme.basic) {
                    authSettings.setValues(AuthenticationType.USER_PWD, null, auth.getPrincipal(), auth.getCredentials());
                    userPwdAuthRBtn.setSelected(true);
                    ((CardLayout) authCards.getLayout()).show(authCards, AuthenticationType.USER_PWD.name());
                }
            } else {
                // No auth settings, default to None
                authSettings.setValues(AuthenticationType.NONE, null, null, null);
                noneAuthRBtn.setSelected(true);
                ((CardLayout) authCards.getLayout()).show(authCards, AuthenticationType.NONE.name());
            }
        }

        userPwdAuthComp.loadCredentials(getCredentialsProvider());
        credentialsAuthComp.loadCredentials(getCredentialsProvider());
    }

    /**
     * @return connector config.
     */
    private ConnectorConfig buildConnector() throws InvalidSettingsException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setLocation(buildUri());
        config.setMaxConnectionPoolSize(getInt(maxConnectionPoolSize.getValue()));

        config.setUsedDefaultDbName(usedDefaultDbName);

        if (usedDefaultDbName) {
            config.setDatabase(null);
        } else if (this.database.getText() == null) {
            config.setDatabase(DEFAULT_DATABASE_NAME);
        } else {
            config.setDatabase(this.database.getText());
        }

        //authentication
        final AuthenticationType authType = authSettings.getAuthenticationType();

        AuthConfig auth = null;

        // Determine AuthConfig based on selected authentication type
        switch (authType) {
            case CREDENTIALS:
                auth = new AuthConfig();
                if (OAUTH2_CREDENTIAL_IDENTIFIER.equals(authSettings.getCredential())) {
                    auth.setScheme(AuthScheme.OAuth2);
                    auth.setPrincipal(OAUTH2_CREDENTIAL_IDENTIFIER); // Store the flag
                    auth.setCredentials(null); // Token will be resolved in ConnectorModel
                } else {
                    auth.setScheme(AuthScheme.flowCredentials);
                    auth.setPrincipal(authSettings.getCredential());
                }
                break;
            case USER_PWD:
                auth = new AuthConfig();
                auth.setScheme(AuthScheme.basic);
                auth.setPrincipal(authSettings.getUserName(getCredentialsProvider()));
                String password = authSettings.getPassword(getCredentialsProvider());
                auth.setCredentials(password != null ? password : "");
                break;
            case NONE:
                auth = null;
                break;
            default:
                throw new RuntimeException("Unexpected auth type: " + authType);
        }

        config.setAuth(auth);
        return config;
    }

    private static int getInt(final Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return 0;
    }

    private URI buildUri() throws InvalidSettingsException {
        final String text = this.url.getText();
        try {
            return new URI(text);
        } catch (final URISyntaxException e) {
            throw new InvalidSettingsException("Invalid URI: " + text);
        }
    }

}
