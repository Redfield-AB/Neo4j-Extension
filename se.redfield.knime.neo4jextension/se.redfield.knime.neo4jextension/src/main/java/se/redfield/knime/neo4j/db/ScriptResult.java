/**
 *
 */
package se.redfield.knime.neo4j.db;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ScriptResult<R> {
    private R result;
    private Throwable exception;

    /**
     * Default constructor.
     */
    public ScriptResult() {
        super();
    }
    /**
     * @param result result.
     */
    public ScriptResult(final R result) {
        super();
        setResult(result);
    }
    /**
     * @param result
     * @param e exception.
     */
    public ScriptResult(final R result, final RuntimeException e) {
        super();
        setResult(result);
        setException(e);
    }
    public R getResult() {
        return result;
    }
    public void setResult(final R result) {
        this.result = result;
    }
    public Throwable getException() {
        return exception;
    }
    public void setException(final Throwable exception) {
        this.exception = exception;
    }
}
