/**
 *
 */
package se.redfield.knime.neo4j.async;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@FunctionalInterface
public interface AsyncRunner<R, V> {
    /**
     * @param number argument number in the incoming argument list.
     * @param arg argument.
     * @return result.
     * @throws Exception in case of error. The method can as return result with exception
     * as simple throw it. It will correct catch and converted to script result with
     * exception and null result.
     */
    RunResult<R> run(int number, V arg) throws Exception;
    /**
     */
    default void workerStopped() {}
    /**
     */
    default void workerStarted() {}
}
