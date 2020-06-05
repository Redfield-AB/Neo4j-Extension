/**
 *
 */
package se.redfield.knime.neo4j.db;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@FunctionalInterface
public interface AsyncRunner<R, V> {
    /**
     * @param arg argument.
     * @return result.
     * @throws Exception in case of error. The method can as return result with exception
     * as simple throw it. It will correct catch and converted to script result with
     * exception and null result.
     */
    ScriptResult<R> run(V arg) throws Exception;
}
