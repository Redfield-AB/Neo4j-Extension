/**
 *
 */
package se.redfield.knime.junit;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * This test runner is developed only for start from eclipse IDE
 * with configured class path.
 * This runner not starts an OSGi framework in fact therefore
 * possible unexpected behavior
 *
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class KnimeTestRunner extends BlockJUnit4ClassRunner {

    /**
     * @param klass testing class.
     * @throws InitializationError
     */
    public KnimeTestRunner(final Class<?> klass) throws InitializationError {
        super(klass);
    }

    /* (non-Javadoc)
     * @see org.junit.runners.BlockJUnit4ClassRunner#createTest()
     */
    @Override
    protected Object createTest() throws Exception {
        final Object test = super.createTest();
        return test;
    }
}
