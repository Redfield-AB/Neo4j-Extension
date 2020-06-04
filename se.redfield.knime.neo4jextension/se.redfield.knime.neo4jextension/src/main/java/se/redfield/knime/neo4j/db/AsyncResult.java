/**
 *
 */
package se.redfield.knime.neo4j.db;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AsyncResult<R> {
    private R result;
    private Throwable exception;

    /**
     * Default constructor.
     */
    public AsyncResult() {
        super();
    }
    /**
     * @param result result.
     */
    public AsyncResult(final R result) {
        super();
        setResult(result);
    }
    /**
     * @param result
     * @param e exception.
     */
    public AsyncResult(final R result, final RuntimeException e) {
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
