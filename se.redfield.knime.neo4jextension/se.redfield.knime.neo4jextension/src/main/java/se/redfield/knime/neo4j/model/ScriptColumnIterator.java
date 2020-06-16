/**
 *
 */
package se.redfield.knime.neo4j.model;

import java.util.Iterator;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ScriptColumnIterator implements Iterator<String> {
    private FlowVariablesProvider varsProvider;
    private CloseableRowIterator iterator;
    private int column;

    public ScriptColumnIterator(final BufferedDataTable inputTable,
            final String inputColumn, final FlowVariablesProvider varsProvider) {
        this.varsProvider = varsProvider;
        this.iterator = inputTable.iterator();
        column = inputTable.getDataTableSpec().findColumnIndex(inputColumn);
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }
    @Override
    public String next() {
        final DataRow row = iterator.next();
        String script = null;
        if (column > -1) {
            final StringCell cell = (StringCell) row.getCell(column);
            script = ModelUtils.insertFlowVariables(cell.getStringValue(), varsProvider);
        }

        if (!iterator.hasNext()) {
            iterator.close();
        }
        return script;
    }
}
