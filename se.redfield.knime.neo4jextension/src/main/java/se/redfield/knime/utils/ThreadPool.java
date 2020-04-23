/**
 *
 */
package se.redfield.knime.utils;

import java.util.concurrent.AbstractExecutorService;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ThreadPool {
    private static final AbstractExecutorService EXECUTOR = new ExecutorServiceImpl("Neo4J Thread Pool");

    public static AbstractExecutorService getExecutor() {
        return EXECUTOR;
    }
}
