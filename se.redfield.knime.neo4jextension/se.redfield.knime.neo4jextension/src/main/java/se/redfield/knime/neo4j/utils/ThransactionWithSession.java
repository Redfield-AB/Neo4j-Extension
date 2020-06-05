/**
 *
 */
package se.redfield.knime.neo4j.utils;

import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ThransactionWithSession {
    private final Session session;
    private final Transaction transaction;

    /**
     * @param session Neo4j session.
     * @param transaction Neo4j transaction.
     */
    public ThransactionWithSession(final Session session, final Transaction transaction) {
        super();
        this.session = session;
        this.transaction = transaction;
    }

    /**
     * @return the session
     */
    public Session getSession() {
        return session;
    }
    /**
     * @return the transaction
     */
    public Transaction getTransaction() {
        return transaction;
    }
    public void commitAndClose() {
        try {
            try {
                transaction.commit();
                transaction.close();
            } finally {
                session.close();
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
    public void rollbackAndClose() {
        try {
            try {
                transaction.rollback();
                transaction.close();
            } finally {
                session.close();
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
