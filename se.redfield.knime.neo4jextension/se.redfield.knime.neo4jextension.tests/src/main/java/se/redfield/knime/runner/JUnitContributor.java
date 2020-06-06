/**
 *
 */
package se.redfield.knime.runner;

import org.eclipse.core.runtime.IContributor;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class JUnitContributor implements IContributor {
    public static final IContributor INSTANCE = new JUnitContributor();

    /**
     * Default constructor.
     */
    private JUnitContributor() {
        super();
    }

    @Override
    public String getName() {
        return "JUnit Test Runner";
    }
}
