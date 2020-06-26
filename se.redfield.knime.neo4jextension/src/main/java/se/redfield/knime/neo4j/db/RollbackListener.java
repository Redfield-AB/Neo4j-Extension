/**
 *
 */
package se.redfield.knime.neo4j.db;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@FunctionalInterface
public interface RollbackListener {
    /**
     */
    void isRolledBack();
}
