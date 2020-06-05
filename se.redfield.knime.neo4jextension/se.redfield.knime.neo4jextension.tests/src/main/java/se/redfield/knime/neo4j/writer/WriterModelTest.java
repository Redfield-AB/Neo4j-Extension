/**
 *
 */
package se.redfield.knime.neo4j.writer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import se.redfield.knime.runner.KnimeTestRunner;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@RunWith(KnimeTestRunner.class)
public class WriterModelTest {
    private WriterModel model;

    /**
     * Default constructor.
     */
    public WriterModelTest() {
        super();
    }

    @Before
    public void setUp() {
        model = new WriterModel();
    }

    @Test
    public void testLoadSettings() {
        model.getInputPortRoles();
    }
}
