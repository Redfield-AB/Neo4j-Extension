/**
 *
 */
package se.redfield.knime.neo4j.db;

import org.neo4j.driver.Session;

import se.redfield.knime.neo4j.async.RunResult;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public interface WithSessionAsyncRunner<R, V> {
    RunResult<R> run(Session session, int number, V arg) throws Exception;
}
