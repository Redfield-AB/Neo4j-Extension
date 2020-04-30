/**
 *
 */
package se.redfield.knime.neo4j.db;

import org.neo4j.driver.Session;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@FunctionalInterface
public interface WithSessionAsyncRunnable<R> {
    void run(Session session);
}
