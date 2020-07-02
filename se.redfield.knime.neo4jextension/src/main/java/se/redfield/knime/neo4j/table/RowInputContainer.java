/**
 *
 */
package se.redfield.knime.neo4j.table;

import org.knime.core.data.DataTable;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.RowInput;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class RowInputContainer {
    private final RowInput input;
    private final long rowCount;

    public RowInputContainer(final DataTable table) {
        super();
        final DataTableRowInput in = new DataTableRowInput(table);
        input = in;
        rowCount = in.getRowCount();
    }
    public RowInputContainer(final RowInput in) {
        super();
        this.input = in;
        if (in instanceof DataTableRowInput) {
            rowCount = ((DataTableRowInput) in).getRowCount();
        } else {
            rowCount = -1;
        }
    }

    public boolean hasRowCount() {
        return rowCount > -1;
    }
    /**
     * @return the input
     */
    public RowInput getInput() {
        return input;
    }
    /**
     * @return the rowCount
     */
    public long getRowCount() {
        return rowCount;
    }
}
