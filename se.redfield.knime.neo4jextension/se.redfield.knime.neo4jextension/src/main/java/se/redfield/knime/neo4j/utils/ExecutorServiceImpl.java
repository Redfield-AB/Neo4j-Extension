/**
 *
 */
package se.redfield.knime.neo4j.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ExecutorServiceImpl extends AbstractExecutorService {
    private enum ThreadState {
        Free,
        Working
    }

    private final LinkedList<Runnable> taskQueue = new LinkedList<>();
    private final Map<Thread, ThreadState> workingThreads = new HashMap<>();
    private int usedThreadPoolSize;
    private int usedCoreThreadPoolSize;
    private int threadPoolSize;
    private final String name;
    private boolean isRunning = false;
    private long maxIddleTime = 30 * 1000l; //30 seconds
    private long usedMaxIddleTime; //30 seconds
    private int coreThreadPoolSize;

    public ExecutorServiceImpl(final String name) {
        setThreadPoolSize(10);
        this.name = name;
    }

    @Override
    public void shutdown() {
        shutdownNow();
    }

    @Override
    public List<Runnable> shutdownNow() {
        final List<Runnable> rejectedTasks = new LinkedList<>();

        synchronized (taskQueue) {
            isRunning = false;
            rejectedTasks.addAll(taskQueue);

            taskQueue.clear();
            taskQueue.notifyAll();

            final long startFinishing = System.currentTimeMillis();
            while (workingThreads.size() > 0) {
                try {
                    taskQueue.wait(3000l);
                } catch (final InterruptedException e) {
                }
                if (System.currentTimeMillis() - startFinishing > 60000l) {
                    break;
                }
            }

            //interrupt all not stopped threads
            final Iterator<Thread> iter = workingThreads.keySet().iterator();
            while(iter.hasNext()) {
                final Thread next = iter.next();
                iter.remove();
                next.interrupt();
            }
        }

        return rejectedTasks;
    }

    @Override
    public boolean isShutdown() {
        synchronized (taskQueue) {
            return !isRunning && workingThreads.isEmpty();
        }
    }

    @Override
    public boolean isTerminated() {
        synchronized (taskQueue) {
            return !isRunning;
        }
    }

    @Override
    public boolean awaitTermination(final long originTimeOut, final TimeUnit unit)
            throws InterruptedException {
        final long timeOut = TimeUnit.MILLISECONDS.convert(originTimeOut, unit);
        final long t0 = System.currentTimeMillis();
        synchronized (taskQueue) {
            while (true) {
                final boolean isThreadsStopped = workingThreads.isEmpty();
                if (isThreadsStopped || System.currentTimeMillis() < t0 + timeOut) {
                    return isThreadsStopped;
                }
            }
        }
    }

    @Override
    public void execute(final Runnable task) {
        synchronized (taskQueue) {
            taskQueue.add(task);
            taskQueue.notify();
        }
        //live synchronized block. It allows the idled threads to be resumed
        //and process the job
        synchronized (taskQueue) {
            possibleStartAdditionalThreads(1);
        }
    }

    public void start() {
        synchronized (taskQueue) {
            if (isRunning) {
                return;
            }

            isRunning = true;
            usedMaxIddleTime = maxIddleTime;
            usedCoreThreadPoolSize = Math.max(1, coreThreadPoolSize);
            usedThreadPoolSize = Math.max(usedCoreThreadPoolSize, getThreadPoolSize());

            possibleStartAdditionalThreads(usedCoreThreadPoolSize);
        }
    }

    private void possibleStartAdditionalThreads(final int numThreads) {
        if (numThreads < 1) {
            return;
        }

        synchronized (taskQueue) {
            int numFree = 0;
            for (final ThreadState s : workingThreads.values()) {
                if (s == ThreadState.Free) {
                    numFree++;
                }
            }

            final int count = Math.min(usedThreadPoolSize - workingThreads.size(),
                    numThreads - numFree);
            for (int i = 0; i < count; i++) {
                startNewWorkerThread();
            }
        }
    }

    private void startNewWorkerThread() {
        final Thread t = new Thread(() ->  {
            runWorkerThread();
        }, name + "-worker");
        workingThreads.put(t, ThreadState.Working);
        t.start();
    }

    private void runWorkerThread() {
        Long startWait = null;
        while (true) {
            Runnable nextTask = null;
            synchronized (taskQueue) {
                //if is not running should stop immediately.
                boolean shouldStop = !isRunning;
                //if queue size is 0 and number of threads more then core thread pool
                //size than may be need to stop current thread.
                final boolean waitTimeExceed = startWait != null
                        && System.currentTimeMillis() - startWait > usedMaxIddleTime;
                if (!shouldStop && waitTimeExceed) {
                    //stop only if current thread is waiting too long
                    shouldStop = (taskQueue.size() == 0
                            && workingThreads.size() > usedCoreThreadPoolSize);
                }

                if (shouldStop) {
                    workingThreads.remove(Thread.currentThread());
                    taskQueue.notifyAll();
                    break;
                } else if (waitTimeExceed) {
                    //start of count wait time again.
                    startWait = null;
                }

                nextTask = taskQueue.size() > 0 ? taskQueue.removeFirst() : null;
                if (nextTask == null) {
                    //mark given thread as free
                    workingThreads.put(Thread.currentThread(), ThreadState.Free);

                    //if not already waiting, then need to start of wait
                    if (startWait == null) {
                        startWait = System.currentTimeMillis();
                    }

                    //wait two seconds
                    final long waitReminder = usedMaxIddleTime
                            - (System.currentTimeMillis()  - startWait);
                    if (waitReminder > 0) {
                        try {
                            taskQueue.wait(waitReminder);
                        } catch (final InterruptedException e) {
                        }
                    }
                } else {
                    startWait = null;
                }
            }

            if (nextTask != null) {
                workingThreads.put(Thread.currentThread(), ThreadState.Working);
                nextTask.run();
            }
        }
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    public void setThreadPoolSize(final int threadPoolSize) {
        if (threadPoolSize < 1) {
            throw new RuntimeException(
                    "Thread pool size should be not less then 1");
        }
        this.threadPoolSize = threadPoolSize;
    }
    public void setCoreThreadPoolSize(final int coreThreadPoolSize) {
        this.coreThreadPoolSize = coreThreadPoolSize;
    }
    public int getCoreThreadPoolSize() {
        return coreThreadPoolSize;
    }
    public void setMaxIddleTime(final long maxIddleTime) {
        if (maxIddleTime < 0) {
            throw new RuntimeException("Negative max iddle time");
        }
        this.maxIddleTime = maxIddleTime;
    }
    public long getMaxIddleTime() {
        return maxIddleTime;
    }
}
