/**
 *
 */
package se.redfield.knime.neo4j.utils;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.streamable.RowInput;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class TestRowInput extends RowInput {
    private final DataTableSpec spec;
    private final RowIterator iterator;

    /**
     * @param table data table.
     *
     */
    public TestRowInput(final BufferedDataTable table) {
        super();
        this.spec = table.getDataTableSpec();
        this.iterator = table.iterator();
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return spec;
    }

    @Override
    public DataRow poll() throws InterruptedException {
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    @Override
    public void close() {
        if (iterator instanceof CloseableRowIterator) {
            ((CloseableRowIterator)iterator).close();
        }
    }

    public long getRowCount() {
        return -1;
    }
}
