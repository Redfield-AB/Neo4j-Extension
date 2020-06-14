/**
 *
 */
package se.redfield.knime.neo4j.db;

import org.neo4j.driver.Session;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public interface WithSessionAsyncRunner<V> {
    void run(Session session, long number, V arg) throws Exception;
}
