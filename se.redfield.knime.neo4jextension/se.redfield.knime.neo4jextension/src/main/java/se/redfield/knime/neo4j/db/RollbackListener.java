/**
 *
 */
package se.redfield.knime.neo4j.db;

import java.util.List;

import org.neo4j.driver.summary.Notification;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@FunctionalInterface
public interface RollbackListener {
    /**
     * @param notifications list of notifications.
     */
    void isRolledBack(List<Notification> notifications);
}
