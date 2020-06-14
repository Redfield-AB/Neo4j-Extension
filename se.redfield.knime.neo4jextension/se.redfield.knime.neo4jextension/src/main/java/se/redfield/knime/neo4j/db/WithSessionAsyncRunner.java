/**
 *
 */
package se.redfield.knime.neo4j.db;

import org.neo4j.driver.Session;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public interface WithSessionAsyncRunner<R, V> {
    RunResult<R> run(Session session, V arg) throws Exception;
}
