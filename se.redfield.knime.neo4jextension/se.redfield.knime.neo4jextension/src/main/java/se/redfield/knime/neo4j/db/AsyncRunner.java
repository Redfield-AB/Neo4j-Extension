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
     * @param threadId worker thread ID.
     * @param arg argument.
     * @return result.
     * @throws Exception in case of error. The method can as return result with exception
     * as simple throw it. It will correct catch and converted to script result with
     * exception and null result.
     */
    RunResult<R> run(long threadId, V arg) throws Exception;
    /**
     * @param threadId worker thread ID.
     */
    default void workerStopped(final long threadId) {}
    /**
     * @param threadId worker thread ID.
     */
    default void workerStarted(final long threadId) {}
}
