/**
 *
 */
package se.redfield.knime.neo4j.async;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class RunResult<T> extends NumberedValue<T> {
    private final boolean isError;
    public RunResult(final long num, final T arg, final boolean isError) {
        super(num, arg);
        this.isError = isError;
    }
    public boolean isError() {
        return isError;
    }
}
