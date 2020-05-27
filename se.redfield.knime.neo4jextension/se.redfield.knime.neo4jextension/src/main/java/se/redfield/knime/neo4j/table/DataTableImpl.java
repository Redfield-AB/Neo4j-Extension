/**
 *
 */
package se.redfield.knime.neo4j.table;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class DataTableImpl implements DataTable {
    private final DataTableSpec spec;
    private final RowIterator iterator;

    public DataTableImpl(final DataTableSpec spec, final RowIterator iterator) {
        super();
        this.spec = spec;
        this.iterator = iterator;
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return spec;
    }
    @Override
    public RowIterator iterator() {
        return iterator;
    }
}
