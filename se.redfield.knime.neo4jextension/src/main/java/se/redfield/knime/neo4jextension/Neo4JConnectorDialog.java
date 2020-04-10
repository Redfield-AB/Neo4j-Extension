/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
        addTab("Encrypting", createSecurityPage());
        addTab("Authentication", createAuthenticationPage());
    }

    /**
     * @return
     */
    private Component createAuthenticationPage() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.RED);
        return p;
    }
    /**
     * @return
     */
    private Component createSecurityPage() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.GREEN);
        return p;
    }
    /**
     * @return settings editor page.
     */
    private JPanel createSettingsPage() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.BLUE);
        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        Neo4JConnectorModel model = buildModel();
        if (model != null) {
            model.saveSettingsTo(settings);
        }
    }
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
        Neo4JConnectorModel model = new Neo4JConnectorModel();
        try {
            model.validateSettings(settings);
            model.loadValidatedSettingsFrom(settings);
            init(model);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException("Failed to load configuration from settings", e);
        }
    }

    /**
     * @param model
     */
    private void init(final Neo4JConnectorModel model) {
        // TODO set fields from model.

    }
    /**
     * @return
     */
    private Neo4JConnectorModel buildModel() {
        Neo4JConnectorModel model = new Neo4JConnectorModel();
        // TODO
        return model;
    }
}
