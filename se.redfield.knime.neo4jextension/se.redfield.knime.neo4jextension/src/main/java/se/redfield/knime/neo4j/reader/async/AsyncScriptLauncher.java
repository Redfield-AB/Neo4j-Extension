/**
 *
 */
package se.redfield.knime.neo4j.reader.async;

import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;

import se.redfield.knime.neo4j.db.DataAdapter;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.reader.FlowVariablesProvider;
import se.redfield.knime.neo4j.reader.ReaderModel;
import se.redfield.knime.neo4j.reader.UiUtils;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncScriptLauncher implements AsyncLauncher {
    private final Neo4jSupport neo4j;
    private final BufferedDataTable inputTable;
    private final int columnForExecute;
    private final AsyncOutputRowIterator output;
    private final FlowVariablesProvider flowVariablesProvider;
    private boolean stopOnAnyError = false;

    //execution scope variables
    private Driver driver;
    private CloseableRowIterator iterator;
    long pos;

    public AsyncScriptLauncher(final Neo4jSupport neo4j, final BufferedDataTable inputTable,
            final AsyncOutputRowIterator output, final FlowVariablesProvider flowWars,
            final int columnForExecute) {
        super();
        this.neo4j = neo4j;
        this.inputTable = inputTable;
        this.columnForExecute = columnForExecute;
        this.output = output;
        this.flowVariablesProvider = flowWars;
    }
    @Override
    public void execute() throws StopException {
        ensureInitialized();

        long currentPos;
        DataRow next;
        synchronized (this) {
            if (!isOpened() || !iterator.hasNext()) {
                mayBeClose();
                throw new StopException();
            }

            currentPos = pos;
            next = iterator.next();
            pos++;
        }

        try {
            final DataRow result = processNewRow(next);
            output.addRow(currentPos, result);
        } catch (final Throwable exc) {
            handleError(null, exc);
            if (stopOnAnyError) {
                output.setError(exc);
                mayBeClose();
            } else {
                output.addRow(currentPos, createDataRow(next, null));
            }
        }
    }
    private DataRow createDataRow(final DataRow originRow, final String json) {
        if (json != null) {
            try {
                return new AppendedColumnRow(originRow, JSONCellFactory.create(json, false));
            } catch (final IOException e) {
                handleError("Failed to create JSON cell", e);
            }
        }
        return new AppendedColumnRow(originRow, new MissingCell("Not a value"));
    }
    private DataRow processNewRow(final DataRow originRow) {
        final StringCell cell = (StringCell) originRow.getCell(columnForExecute);

        final List<Record> records = Neo4jSupport.runRead(driver, UiUtils.insertFlowVariables(
                cell.getStringValue(), flowVariablesProvider),
                n -> handleVargings(ReaderModel.buildWarning(n)));

        final String json = ReaderModel.buildJson(records,
                new DataAdapter(driver.defaultTypeSystem()));

        return createDataRow(originRow, json);
    }
    private void mayBeClose() {
        if (isOpened()) {
            // set driver to null
            driver.closeAsync();
            driver = null;

            // but not set iterator to null
            iterator.close();
        }
    }
    private synchronized void ensureInitialized() throws StopException {
        if (!isOpened() && !wasClosed()) {
            try {
                driver = neo4j.createDriver();
                this.iterator = inputTable.iterator();
            } catch (final Throwable e) {
                handleError(null, e);
                output.setError(e);
                throw new StopException();
            }
        }
    }

    private boolean isOpened() {
        return driver != null;
    }
    private boolean wasClosed() {
        return driver == null && iterator != null;
    }
    protected void handleError(final String message, final Throwable e) {
    }
    protected void handleVargings(final String buildWarning) {
    }
    @Override
    protected void finalize() throws Throwable {
        try {
            mayBeClose();
        } catch (final Throwable e) {
        } finally {
            super.finalize();
        }
    }
    public void setStopOnAnyError(final boolean stop) {
        this.stopOnAnyError = stop;
    }
    public boolean isStopOnAnyError() {
        return stopOnAnyError;
    }
}
