/**
 *
 */
package se.redfield.knime.neo4j.utils;

import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.streamable.RowOutput;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class TestRowOutput extends RowOutput {
    private BufferedDataContainer table;

    /**
     * @param table
     */
    public TestRowOutput(final BufferedDataContainer table) {
        this.table = table;
    }
    @Override
    public void push(final DataRow row) throws InterruptedException {
        table.addRowToTable(row);
    }
    @Override
    public void close() throws InterruptedException {
        table.close();
    }
}
