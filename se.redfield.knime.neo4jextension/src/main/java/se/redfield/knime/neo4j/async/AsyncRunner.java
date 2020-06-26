/**
 *
 */
package se.redfield.knime.neo4j.async;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@FunctionalInterface
public interface AsyncRunner<V, R> {
    /**
     * @param arg argument.
     * @return result.
     * @throws Throwable
     */
    R run(V arg) throws Throwable;
    /**
     */
    default void workerStopped() {}
    /**
     */
    default void workerStarted() {}
}
