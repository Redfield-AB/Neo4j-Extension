/**
 *
 */
package se.redfield.knime.neo4j.reader;

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
public class ReaderDialogTest {
    private AccessibleReaderDialog dialog;

    /**
     * Default constructor.
     */
    public ReaderDialogTest() {
        super();
    }

    @Before
    public void setUp() {
        dialog = new AccessibleReaderDialog();
    }

    @Test
    public void testSaveLoadSettings() throws NotConfigurableException, InvalidSettingsException {
        final String script = "junit script";
        final boolean useJson = true;

        final ReaderConfig cfg = new ReaderConfig();
        cfg.setScript(script);
        cfg.setUseJson(useJson);

        final NodeSettings s = new NodeSettings("junit");
        final ReaderConfigSerializer ser = new ReaderConfigSerializer();
        ser.save(cfg, s);

        final PortObjectSpec[] specs = {KNimeHelper.createConnectorSpec()};
        dialog.loadSettingsFrom(s, specs);

        final NodeSettings s1 = new NodeSettings("junit");
        dialog.saveSettingsTo(s1);
        final ReaderConfig cfg1 = ser.read(s1);

        assertEquals(cfg.getInputColumn(), cfg1.getInputColumn());
        assertEquals(cfg.isUseJson(), cfg1.isUseJson());
    }
    @Test
    public void testNotConnected() {
        final NodeSettings s = new NodeSettings("junit");
        final ReaderConfigSerializer ser = new ReaderConfigSerializer();
        ser.save(new ReaderConfig(), s);

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
