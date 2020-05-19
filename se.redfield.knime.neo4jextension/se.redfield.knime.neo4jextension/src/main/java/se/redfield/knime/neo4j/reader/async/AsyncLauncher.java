/**
 *
 */
package se.redfield.knime.neo4j.reader.async;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@FunctionalInterface
public interface AsyncLauncher {
    void execute() throws StopException;
}
