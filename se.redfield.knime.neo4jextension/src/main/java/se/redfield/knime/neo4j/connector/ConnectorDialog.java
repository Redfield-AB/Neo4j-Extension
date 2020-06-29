/**
 *
 */
package se.redfield.knime.neo4j.connector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentAuthentication;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;

import se.redfield.knime.neo4j.model.HashGenerator;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorDialog extends NodeDialogPane {
    //settings tab
    private final JTextField url = new JTextField();

    private final SettingsModelAuthentication authSettings = new SettingsModelAuthentication(
            "neo4jAuth", AuthenticationType.USER_PWD, "neo4j", null, null);
    DialogComponentAuthentication authComp = new DialogComponentAuthentication(
            authSettings, "Authentication",
            AuthenticationType.NONE,
            AuthenticationType.CREDENTIALS,
            AuthenticationType.USER_PWD);

    //config
    private final JFormattedTextField maxConnectionPoolSize = createIntValueEditor();

    private String oldPassword;

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

        //connecton
        //URL
        final JPanel north = new JPanel(new GridBagLayout());
        north.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Connection"));
        p.add(north, BorderLayout.NORTH);

        addLabeledComponent(north, "Neo4j URL", url, 0);
        addLabeledComponent(north, "Max connection pool size:", maxConnectionPoolSize, 1);

        //Authentication
        final JPanel center = new JPanel(new BorderLayout(5, 5));
        p.add(center, BorderLayout.CENTER);

        //use auth checkbox
        center.add(authComp.getComponentPanel(), BorderLayout.NORTH);
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
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            final ConnectorConfig model = new ConnectorConfigSerializer().load(settings);
            init(model);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException("Failed to load configuration from settings", e);
        }
    }

    /**
     * @param model
     * @throws NotConfigurableException
     */
    private void init(final ConnectorConfig model) throws NotConfigurableException {
        this.url.setText(model.getLocation() == null
                ? "" : model.getLocation().toASCIIString());
        maxConnectionPoolSize.setValue(model.getMaxConnectionPoolSize());
        oldPassword = null;

        //authentication
        final AuthConfig auth = model.getAuth();

        final boolean shouldUseAuth = auth != null;

        if (!shouldUseAuth) {
            authSettings.setValues(AuthenticationType.NONE,
                    null, null, null);
        } else if (auth.getScheme() == AuthScheme.flowCredentials) {
            authSettings.setValues(AuthenticationType.CREDENTIALS,
                    auth.getPrincipal(), null, null);
        } else {
            final String password = auth.getCredentials();
            this.oldPassword = password;
            authSettings.setValues(AuthenticationType.USER_PWD,
                    null, auth.getPrincipal(), createPasswordHash(auth.getCredentials()));
        }

        authComp.loadCredentials(getCredentialsProvider());
    }
    /**
     * @return connector config.
     */
    private ConnectorConfig buildConnector() throws InvalidSettingsException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setLocation(buildUri());
        config.setMaxConnectionPoolSize(getInt(maxConnectionPoolSize.getValue()));

        //authentication
        final AuthenticationType authType = authSettings.getAuthenticationType();

        AuthConfig auth = null;

        switch (authType) {
            case CREDENTIALS:
                auth = new AuthConfig();
                auth.setScheme(AuthScheme.flowCredentials);
                auth.setPrincipal(authSettings.getCredential());
            break;
            case USER_PWD:
                auth = new AuthConfig();
                auth.setScheme(AuthScheme.basic);
                auth.setPrincipal(authSettings.getUserName(getCredentialsProvider()));

                String password = authSettings.getPassword(getCredentialsProvider());
                //if password not changed save old password
                if (oldPassword != null && createPasswordHash(oldPassword).equals(password)) {
                    password = oldPassword;
                }
                auth.setCredentials(password);
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
    private String createPasswordHash(final String password) {
        if (password == null) {
            return null;
        }

        // first 10 symbols of password hash
        final String hash = HashGenerator.generateHash(password);
        return hash.substring(0, 10);
    }
}
