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

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JConnectorDialog extends NodeDialogPane {
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
    private final JFormattedTextField maxConnectionPoolSize = new JFormattedTextField();

    private final JFormattedTextField idleTimeBeforeConnectionTest = new JFormattedTextField();
    private final JFormattedTextField maxConnectionLifetimeMillis = new JFormattedTextField();
    private final JFormattedTextField connectionAcquisitionTimeoutMillis = new JFormattedTextField();

    private final JFormattedTextField routingFailureLimit = new JFormattedTextField();
    private final JFormattedTextField routingRetryDelayMillis = new JFormattedTextField();
    private final JFormattedTextField fetchSize = new JFormattedTextField();
    private final JFormattedTextField routingTablePurgeDelayMillis = new JFormattedTextField();

    private final JFormattedTextField connectionTimeoutMillis = new JFormattedTextField();

    //max retry time in milliseconds
    private final JFormattedTextField retrySettings = new JFormattedTextField();

    private final JCheckBox isMetricsEnabled = new JCheckBox();
    private final JFormattedTextField eventLoopThreads = new JFormattedTextField();

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
        addTab("Settings", createSettingsPage());
        addTab("Encrypting", createEncriptingPage());
        addTab("Authentication", createAuthenticationPage());

    }

    @Override
    protected void addFlowVariablesTab() {
        // ignore flow variables tab.
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
        this.useAuth.addActionListener(e -> useAuthChanged(p, useAuth.isSelected()));

        useAuthChanged(p, true);
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

    private void useAuthChanged(final JPanel parent, final boolean selected) {
        final String authFieldsContainer = "authFieldsContainer";

        if (!selected) {
            final JPanel container = (JPanel) useAuth.getClientProperty(authFieldsContainer);
            if (container != null) {
                parent.remove(container);
            }
        } else {
            final JPanel container = new JPanel(new GridBagLayout());
            container.setBorder(new CompoundBorder(
                    new BevelBorder(BevelBorder.RAISED),
                    new EmptyBorder(5, 5, 5, 5)));

            addLabeledComponent(container, "Scheme:", scheme, 0);
            addLabeledComponent(container, "Login:", principal, 1);
            addLabeledComponent(container, "Password:", credentials, 2);
            addLabeledComponent(container, "Realm:", realm, 3);
            addLabeledComponent(container, "Parameters:", parameters, 4);

            final JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.add(container, BorderLayout.NORTH);

            parent.add(wrapper, BorderLayout.CENTER);
            useAuth.putClientProperty(authFieldsContainer, wrapper);
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
        final Neo4JConnector model = buildModel();
        if (model != null) {
            model.save(settings);
        }
    }
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
        final Neo4JConnector model = new Neo4JConnector();
        try {
            model.load(settings);
            init(model);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException("Failed to load configuration from settings", e);
        }
    }

    /**
     * @param model
     */
    private void init(final Neo4JConnector model) {
        this.url.setText(model.getLocation() == null
                ? "" : model.getLocation().toASCIIString());
        //authentication
        final AuthConfig auth = model.getAuth();

        final boolean shouldUseAuth = auth != null;
        useAuth.setSelected(shouldUseAuth);
        if (shouldUseAuth) {
            principal.setText(auth.getPrincipal());
            credentials.setText(auth.getCredentials());
        }
    }
    /**
     * @return
     */
    private Neo4JConnector buildModel() throws InvalidSettingsException {
        final Neo4JConnector model = new Neo4JConnector();
        model.setLocation(buildUri());

        //authentication
        if (useAuth.isSelected()) {
            final AuthConfig auth = new AuthConfig();
            auth.setPrincipal(getNotEmpty("user name", this.principal));
            auth.setCredentials(getNotEmpty("password", this.credentials));
            model.setAuth(auth);
        }

        return model;
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
            throw new InvalidSettingsException("Invalid URL: " + text);
        }
    }
}
