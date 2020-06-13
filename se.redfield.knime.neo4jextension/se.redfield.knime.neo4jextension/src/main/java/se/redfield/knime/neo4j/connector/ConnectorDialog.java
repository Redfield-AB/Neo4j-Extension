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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

import se.redfield.knime.neo4j.utils.HashGenerator;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ConnectorDialog extends NodeDialogPane {
    private static final String OLD_PASSWORD = "OLD_PASSWORD";
    private static final String USE_AUTH_PARENT_PANEL = "useAuthParentPanel";

    //settings tab
    private final JTextField url = new JTextField();

    //authentication
    private final JCheckBox useAuth = new JCheckBox();
    private final JComboBox<AuthScheme> scheme = new JComboBox<AuthScheme>();
    private final JTextField principal = new JTextField();
    private final JPasswordField credentials = new JPasswordField();
    private final JComboBox<String> flowCredentials = new JComboBox<>();

    //config
    private final JFormattedTextField maxConnectionPoolSize = createIntValueEditor();

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

        //URL
        final JPanel north = new JPanel(new GridBagLayout());
        p.add(north, BorderLayout.NORTH);

        addLabeledComponent(north, "Neo4j URL", url, 0);
        addLabeledComponent(north, "Max connection pool size:", maxConnectionPoolSize, 1);

        //Authentication
        final JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Authentication:"));
        p.add(center, BorderLayout.CENTER);

        //use auth checkbox
        final JPanel useAuthPane = new JPanel(new BorderLayout(5, 5));
        useAuthPane.add(new JLabel("Use authentication"), BorderLayout.WEST);
        useAuthPane.add(this.useAuth, BorderLayout.CENTER);

        center.add(useAuthPane, BorderLayout.NORTH);

        this.useAuth.setSelected(true);
        this.useAuth.putClientProperty(USE_AUTH_PARENT_PANEL, center);
        this.useAuth.addActionListener(e -> useAuthChanged(useAuth.isSelected()));

        useAuthChanged(true);
        return p;
    }

    private void useAuthChanged(final boolean selected) {
        final JPanel parent = (JPanel) useAuth.getClientProperty(USE_AUTH_PARENT_PANEL);
        final String authFieldsContainer = "authFieldsContainer";

        JPanel wrapper = (JPanel) useAuth.getClientProperty(authFieldsContainer);
        if (!selected) {
            if (wrapper != null) {
                parent.remove(wrapper);
            }
        } else {
            if (wrapper == null) {
                final JPanel container = new JPanel(new GridBagLayout());
                container.setBorder(new CompoundBorder(
                        new EtchedBorder(BevelBorder.RAISED),
                        new EmptyBorder(5, 5, 5, 5)));

                scheme.setRenderer(new AuthSchemeCellRenderer());
                //scheme
                for (final AuthScheme s : AuthScheme.values()) {
                    scheme.addItem(s);
                }

                scheme.setSelectedItem(AuthScheme.basic);
                authSchemeChanged(container, AuthScheme.basic);
                scheme.addActionListener(e -> authSchemeChanged(
                        container, (AuthScheme) scheme.getSelectedItem()));

                wrapper = new JPanel(new BorderLayout());
                wrapper.add(container, BorderLayout.NORTH);

                useAuth.putClientProperty(authFieldsContainer, wrapper);
            }

            parent.add(wrapper, BorderLayout.CENTER);
        }

        if (getPanel() != null) {
            getPanel().repaint();
        }
    }

    /**
     * @param container
     * @param s
     */
    private void authSchemeChanged(final JPanel container, final AuthScheme s) {
        container.removeAll();

        addLabeledComponent(container, "Authentication type:", scheme, 0);
        if (s == AuthScheme.flowCredentials) {
            addLabeledComponent(container, "Flow credentials:", flowCredentials, 1);
        } else {
            addLabeledComponent(container, "Username:", principal, 1);
            addLabeledComponent(container, "Password:", credentials, 2);
        }
        if (getPanel() != null) {
            getPanel().repaint();
        }
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
     */
    private void init(final ConnectorConfig model) {
        this.url.setText(model.getLocation() == null
                ? "" : model.getLocation().toASCIIString());
        maxConnectionPoolSize.setValue(model.getMaxConnectionPoolSize());

        //authentication
        final AuthConfig auth = model.getAuth();

        final boolean shouldUseAuth = auth != null;
        useAuth.setSelected(shouldUseAuth);
        useAuthChanged(shouldUseAuth);

        //reload and setup flow credentials
        flowCredentials.removeAllItems();
        for (final String cr : getCredentialsNames()) {
            flowCredentials.addItem(cr);
        }
        if (shouldUseAuth) {
            final AuthScheme authScheme = auth.getScheme();
            this.scheme.setSelectedItem(authScheme);

            if (authScheme != AuthScheme.flowCredentials) {
                principal.setText(auth.getPrincipal());

                final String password = auth.getCredentials();
                credentials.putClientProperty(OLD_PASSWORD, password);
                credentials.setText(createPasswordHash(password));
            } else {
                flowCredentials.setSelectedItem(auth.getPrincipal());
            }
        }
    }
    /**
     * @return connector config.
     */
    private ConnectorConfig buildConnector() throws InvalidSettingsException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setLocation(buildUri());
        config.setMaxConnectionPoolSize(getInt(maxConnectionPoolSize.getValue()));

        //authentication
        if (useAuth.isSelected()) {
            final AuthConfig auth = new AuthConfig();

            final AuthScheme authScheme = (AuthScheme) scheme.getSelectedItem();
            auth.setScheme(authScheme);

            if (authScheme != AuthScheme.flowCredentials) {
                auth.setPrincipal(getNotEmpty("user name", this.principal));

                String password = getNotEmpty("password", this.credentials);
                final String oldPassword = (String) credentials.getClientProperty(OLD_PASSWORD);
                //if password not changed save old password
                if (oldPassword != null && createPasswordHash(oldPassword).equals(password)) {
                    password = oldPassword;
                }
                auth.setCredentials(password);
            } else {
                final String c = (String) this.flowCredentials.getSelectedItem();
                if (c == null) {
                    throw new InvalidSettingsException("Empty flow credentials");
                }
                auth.setPrincipal(c);
                auth.setCredentials(null);
            }

            config.setAuth(auth);
        } else {
            config.setAuth(null);
        }

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
    /**
     * @param name
     * @param comp
     * @return
     * @throws InvalidSettingsException
     */
    private String getNotEmpty(final String name, final JTextComponent comp)
            throws InvalidSettingsException {
        final String text = comp.getText();
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidSettingsException("Invalid " + name + ": " + text);
        }
        return text;
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
