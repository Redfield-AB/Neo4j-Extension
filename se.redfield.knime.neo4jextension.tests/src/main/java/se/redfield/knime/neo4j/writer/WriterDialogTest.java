/**
 *
 */
package se.redfield.knime.neo4j.writer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import junit.framework.AssertionFailedError;
import se.redfield.knime.neo4j.utils.KNimeHelper;
import se.redfield.knime.runner.KnimeTestRunner;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@RunWith(KnimeTestRunner.class)
public class WriterDialogTest {
    private AccessibleWriterDialog dialog;

    /**
     * Default constructor.
     */
    public WriterDialogTest() {
        super();
    }

    @Before
    public void setUp() {
        dialog = new AccessibleWriterDialog();
    }

    @Test
    public void testSaveLoadSettings() throws NotConfigurableException, InvalidSettingsException {
        final String script = "junit script";
        final WriterConfig cfg = new WriterConfig();
        cfg.setScript(script);

        final NodeSettings s = new NodeSettings("junit");
        final WriterConfigSerializer ser = new WriterConfigSerializer();
        ser.save(cfg, s);

        final PortObjectSpec[] specs = {KNimeHelper.createConnectorSpec()};
        dialog.loadSettingsFrom(s, specs);

        final NodeSettings s1 = new NodeSettings("junit");
        dialog.saveSettingsTo(s1);
        final WriterConfig cfg1 = ser.read(s1);

        assertEquals(cfg.getInputColumn(), cfg1.getInputColumn());
    }
    @Test
    public void testNotConnected() {
        final NodeSettings s = new NodeSettings("junit");
        final WriterConfigSerializer ser = new WriterConfigSerializer();
        ser.save(new WriterConfig(), s);

        try {
            dialog.loadSettingsFrom(s, new PortObjectSpec[0]);
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            //ok
        }

        try {
            dialog.loadSettingsFrom(s, new PortObjectSpec[] {null});
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            //ok
        }
    }
}
