/**
 *
 */
package se.redfield.knime.neo4j.ui;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@FunctionalInterface
public interface ValueInsertHandler<T> {
    void insert(T value);
}
