/**
 *
 */
package se.redfield.knime.neo4jextension;

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
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.neo4j.driver.Config.TrustStrategy.Strategy;
import org.neo4j.driver.Driver;

import se.redfield.knime.neo4jextension.cfg.AdvancedSettings;
import se.redfield.knime.neo4jextension.cfg.AuthConfig;
import se.redfield.knime.neo4jextension.cfg.ConnectorConfig;
import se.redfield.knime.neo4jextension.cfg.ConnectorConfigSerializer;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JConnectorDialog extends NodeDialogPane {
    private static final String USE_AUTH_PARENT_PANEL = "useAuthParentPanel";

    //settings tab
    private final JTextField url = new JTextField();

    //authentication
    private final JCheckBox useAuth = new JCheckBox();
    private final JComboBox<String> scheme = new JComboBox<String>();
    private final JTextField principal = new JTextField();
    private final JTextField credentials = new JTextField();
    private final JTextField realm = new JTextField();
    private final JTextField parameters = new JTextField();

    //config
    private final JCheckBox logLeakedSessions = new JCheckBox();
    private final JFormattedTextField maxConnectionPoolSize = createIntValueEditor();

    private final JFormattedTextField idleTimeBeforeConnectionTest = createIntValueEditor();
    private final JFormattedTextField maxConnectionLifetimeMillis = createIntValueEditor();
    private final JFormattedTextField connectionAcquisitionTimeoutMillis = createIntValueEditor();

    private final JFormattedTextField routingFailureLimit = createIntValueEditor();
    private final JFormattedTextField routingRetryDelayMillis = createIntValueEditor();
    private final JFormattedTextField fetchSize = createIntValueEditor();
    private final JFormattedTextField routingTablePurgeDelayMillis = createIntValueEditor();

    private final JFormattedTextField connectionTimeoutMillis = createIntValueEditor();

    //max retry time in milliseconds
    private final JFormattedTextField retrySettings = createIntValueEditor();

    private final JCheckBox isMetricsEnabled = new JCheckBox();
    private final JFormattedTextField eventLoopThreads = createIntValueEditor();

    //security settings
    private final JComboBox<Strategy> strategy = new JComboBox<>();
    private final JTextField certFile = new JTextField();
    private final JCheckBox encrypted = new JCheckBox();
    private final JCheckBox hostnameVerificationEnabled = new JCheckBox();

    /**
     * Default constructor.
     */
    public Neo4JConnectorDialog() {
        super();
        addTab("Advanced Settings", createSettingsPage());
        addTab("Encrypting", createEncriptingPage());
        addTab("Authentication", createAuthenticationPage());

    }

    private JFormattedTextField createIntValueEditor() {
        final JFormattedTextField tf = new JFormattedTextField(NumberFormat.getIntegerInstance());
        tf.setValue(0);
        return tf;
    }
    /**
     * @return
     */
    private Component createAuthenticationPage() {
        final JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(new EmptyBorder(5, 5, 5, 5));

        //use auth checkbox
        final JPanel useAuthPane = new JPanel(new BorderLayout(5, 5));
        useAuthPane.add(new JLabel("Use authentication"), BorderLayout.WEST);
        useAuthPane.add(this.useAuth, BorderLayout.CENTER);

        p.add(useAuthPane, BorderLayout.NORTH);

        this.useAuth.setSelected(true);
        this.useAuth.putClientProperty(USE_AUTH_PARENT_PANEL, p);
        this.useAuth.addActionListener(e -> useAuthChanged(useAuth.isSelected()));

        useAuthChanged(true);
        return p;
    }

    /**
     * @return
     */
    private Component createEncriptingPage() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(5, 5, 5, 5));

        this.encrypted.setSelected(true);
        addLabeledComponent(p, "Use encription", this.encrypted, 0);
        addLabeledComponent(p, "Host name verification enabled",
                this.hostnameVerificationEnabled, 1);

        strategy.setEditable(false);
        for (final Strategy s : Strategy.values()) {
            strategy.addItem(s);
        }
        addLabeledComponent(p, "Use encription", this.strategy, 2);
        addLabeledComponent(p, "Certificate file", this.certFile, 3);

        strategy.addActionListener(e -> strategyChanged());
        strategyChanged();

        final JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(p, BorderLayout.NORTH);
        return wrapper;
    }
    /**
     * @return settings editor page.
     */
    private JPanel createSettingsPage() {
        final JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(5, 5, 5, 5));

        //settings tab
        final JPanel north = new JPanel(new GridBagLayout());
        root.add(north, BorderLayout.NORTH);

        addLabeledComponent(north, "Neo4J URL", url, 0);

        //other config
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new BevelBorder(BevelBorder.RAISED));

        addLabeledComponent(p, "Log leaked sessions:", logLeakedSessions, 0);
        addLabeledComponent(p, "Max connection pool size:", maxConnectionPoolSize, 1);
        addLabeledComponent(p, "Idle time before connection test:", idleTimeBeforeConnectionTest, 2);
        addLabeledComponent(p, "Max connection life time (ms):", maxConnectionLifetimeMillis, 3);
        addLabeledComponent(p, "Connection acquisition time out (ms):",
                connectionAcquisitionTimeoutMillis, 4);
        addLabeledComponent(p, "Routing failure limit (ms):", routingFailureLimit, 5);
        addLabeledComponent(p, "Routing retry delay limit (ms):", routingRetryDelayMillis, 6);
        addLabeledComponent(p, "Fetch size:", fetchSize, 7);
        addLabeledComponent(p, "Routing table purge delay (ms):",
                routingTablePurgeDelayMillis, 8);
        addLabeledComponent(p, "Connection timeout (ms):", connectionTimeoutMillis, 9);
        addLabeledComponent(p, "Max retry time in (ms):", retrySettings, 10);
        addLabeledComponent(p, "Is metrics enabled:", isMetricsEnabled, 11);
        addLabeledComponent(p, "Num event loop threads:", eventLoopThreads, 12);

        final JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(p, BorderLayout.NORTH);

        final JScrollPane sp = new JScrollPane();
        sp.getViewport().add(wrapper);

        root.add(sp, BorderLayout.CENTER);
        return root;
    }

    private void strategyChanged() {
        final Strategy s = (Strategy) strategy.getSelectedItem();
        final boolean certFileEnabled = s == Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES;
        certFile.setEnabled(certFileEnabled);
        certFile.setEditable(certFileEnabled);
        getPanel().repaint();
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
                        new BevelBorder(BevelBorder.RAISED),
                        new EmptyBorder(5, 5, 5, 5)));

                //scheme
                scheme.addItem("basic");
                scheme.addItem("custom");
                scheme.addItem("kerberos");

                scheme.setSelectedItem("basic");
                addLabeledComponent(container, "Scheme:", scheme, 0);

                addLabeledComponent(container, "Login:", principal, 1);
                addLabeledComponent(container, "Password:", credentials, 2);
                addLabeledComponent(container, "Realm:", realm, 3);
                addLabeledComponent(container, "Parameters:", parameters, 4);

                wrapper = new JPanel(new BorderLayout());
                wrapper.add(container, BorderLayout.NORTH);

                useAuth.putClientProperty(authFieldsContainer, wrapper);
            }

            parent.add(wrapper, BorderLayout.CENTER);
        }

        getPanel().repaint();
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
        //authentication
        final AuthConfig auth = model.getAuth();

        final boolean shouldUseAuth = auth != null;
        useAuth.setSelected(shouldUseAuth);
        useAuthChanged(shouldUseAuth);

        if (shouldUseAuth) {
            principal.setText(auth.getPrincipal());
            credentials.setText(auth.getCredentials());
            scheme.setSelectedItem(auth.getScheme());
        }

        //setting
        final AdvancedSettings cfg = model.getAdvancedSettings();
        logLeakedSessions.setSelected(cfg.isLogLeakedSessions());
        maxConnectionPoolSize.setValue(cfg.getMaxConnectionPoolSize());

        idleTimeBeforeConnectionTest.setValue(cfg.getIdleTimeBeforeConnectionTest());
        maxConnectionLifetimeMillis.setValue(cfg.getMaxConnectionLifetimeMillis());
        connectionAcquisitionTimeoutMillis.setValue(cfg.getConnectionAcquisitionTimeoutMillis());

        routingFailureLimit.setValue(cfg.getRoutingFailureLimit());
        routingRetryDelayMillis.setValue(cfg.getRoutingRetryDelayMillis());
        fetchSize.setValue(cfg.getFetchSize());
        routingTablePurgeDelayMillis.setValue(cfg.getRoutingTablePurgeDelayMillis());

        connectionTimeoutMillis.setValue(cfg.getConnectionTimeoutMillis());

        //max retry time in milliseconds
        retrySettings.setValue(cfg.getRetrySettings());

        eventLoopThreads.setValue(cfg.getEventLoopThreads());
    }
    /**
     * @return
     */
    private ConnectorConfig buildConnector() throws InvalidSettingsException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setLocation(buildUri());

        //authentication
        if (useAuth.isSelected()) {
            final AuthConfig auth = new AuthConfig();
            auth.setScheme((String) scheme.getSelectedItem());
            auth.setPrincipal(getNotEmpty("user name", this.principal));
            auth.setCredentials(getNotEmpty("password", this.credentials));
            config.setAuth(auth);
        }

        //settings
        final AdvancedSettings cfg = new AdvancedSettings();

        cfg.setLogLeakedSessions(logLeakedSessions.isSelected());
        cfg.setMaxConnectionPoolSize(getInt(maxConnectionPoolSize.getValue()));

        cfg.setIdleTimeBeforeConnectionTest(getLong(idleTimeBeforeConnectionTest.getValue()));
        cfg.setMaxConnectionLifetimeMillis(getLong(maxConnectionLifetimeMillis.getValue()));
        cfg.setConnectionAcquisitionTimeoutMillis(getLong(connectionAcquisitionTimeoutMillis.getValue()));

        cfg.setRoutingFailureLimit(getInt(routingFailureLimit.getValue()));
        cfg.setRoutingRetryDelayMillis(getLong(routingRetryDelayMillis.getValue()));
        cfg.setFetchSize(getInt(fetchSize.getValue()));
        cfg.setRoutingTablePurgeDelayMillis(getLong(routingTablePurgeDelayMillis.getValue()));

        cfg.setConnectionTimeoutMillis(getLong(connectionTimeoutMillis.getValue()));

        //max retry time in milliseconds
        cfg.setRetrySettings(getLong(retrySettings.getValue()));
        cfg.setEventLoopThreads(getInt(eventLoopThreads.getValue()));

        config.setAdvancedSettings(cfg);

        testConnection(config);
        return config;
    }

    private long getLong(final Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value != null) {
            return Long.parseLong(value.toString());
        }
        return 0;
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
     * @param c connector to test.
     */
    private void testConnection(final ConnectorConfig c) throws InvalidSettingsException {
        try {
            final Driver driver = ConnectorPortObject.createDriver(c);
            try {
                driver.verifyConnectivity();
            } finally {
                driver.close();
            }
        } catch (final Throwable e) {
            throw new InvalidSettingsException(e.getMessage());
        }
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
}
