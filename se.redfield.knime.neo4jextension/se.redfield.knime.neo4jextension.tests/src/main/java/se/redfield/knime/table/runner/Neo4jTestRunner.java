/**
 *
 */
package se.redfield.knime.table.runner;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4jTestRunner extends BlockJUnit4ClassRunner {
    private static final String DBUSER = "neo4j";
    private static final String PASSWORD = "password";
    private static final String URL = "bolt://localhost:7687";
    private static final DisableCertsClassLoader loader = new DisableCertsClassLoader();

    /**
     * @param klass testing class.
     */
    public Neo4jTestRunner(final Class<?> klass) throws InitializationError {
        super(loadTestClass(klass.getName()));
        final Neo4JTestContext ctxt = new Neo4JTestContext(URL, DBUSER, PASSWORD);
        Neo4JTestContext.setCurrent(ctxt);
    }

    private static Class<?> loadTestClass(final String name) throws InitializationError {
        try {
            return loader.loadClass(name);
        } catch (final ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            Neo4JTestContext.destroy();
        } catch (final Throwable e) {
        }
        super.finalize();
    }
}
