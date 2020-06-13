/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import se.redfield.knime.neo4j.db.AsyncRunner;
import se.redfield.knime.neo4j.db.RunResult;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public abstract class AsyncRunnerWithSession<R, V> implements AsyncRunner<R, V> {
    private Driver driver;
    private Map<Long, Session> sessions = new ConcurrentHashMap<>();

    public AsyncRunnerWithSession(final Driver d) {
        super();
        this.driver = d;
    }
    @Override
    public void workerStarted() {
        //create session for current worker thread.
        sessions.put(getThreadId(), driver.session());
    }
    @Override
    public RunResult<R> run(final V arg) throws Exception {
        return run(sessions.get(getThreadId()), arg);
    }
    protected abstract RunResult<R> run(Session session, V arg);
    @Override
    public void workerStopped() {
        //close session for current worker thread
        final Session session = sessions.remove(getThreadId());
        if (session != null) {
            session.close();
        }
    }
    private static long getThreadId() {
        return Thread.currentThread().getId();
    }
}
