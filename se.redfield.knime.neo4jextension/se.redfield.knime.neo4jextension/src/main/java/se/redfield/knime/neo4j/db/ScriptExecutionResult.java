/**
 *
 */
package se.redfield.knime.neo4j.db;

import java.util.List;

import org.knime.core.data.DataRow;
import org.neo4j.driver.Record;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ScriptExecutionResult {
    public final DataRow row;
    public final List<Record> recors;
    public final Throwable error;
    public ScriptExecutionResult(final DataRow row, final List<Record> recors, final Throwable error) {
        super();
        this.row = row;
        this.recors = recors;
        this.error = error;
    }
}
