/**
 *
 */
package se.redfield.knime.neo4j.reader.async;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncOutputRowIterator extends RowIterator {
    private long count = 0;
    private final Object sync = new Object();
    private final long size;
    private final Map<Long, DataRow> rows = new HashMap<>();

    private Throwable error;

    public AsyncOutputRowIterator(final long outputSize) {
        this.size = outputSize;
    }

    @Override
    public boolean hasNext() {
        synchronized (sync) {
            checkError();
            return count < size;
        }
    }
    private void checkError() {
        if (error != null) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public DataRow next() {
        synchronized (sync) {
            while (hasNext()) {
                if (rows.isEmpty() || !rows.containsKey(this.count)) {
                    try {
                        sync.wait();
                    } catch (final InterruptedException e) {
                        throw new RuntimeException("Current thread is interrupted");
                    }
                } else {
                    final long c = count;
                    count++;
                    return rows.remove(c);
                }
            }
        }
        return null;
    }
    public void addRow(final long number, final DataRow r) {
        synchronized (sync) {
            rows.put(number, r);
            sync.notify();
        }
    }
    public void setError(final Throwable error) {
        synchronized (sync) {
            this.error = error;
            sync.notify();
        }
    }
}
