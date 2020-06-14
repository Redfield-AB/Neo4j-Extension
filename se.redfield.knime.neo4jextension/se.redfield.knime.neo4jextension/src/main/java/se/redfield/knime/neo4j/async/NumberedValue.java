/**
 *
 */
package se.redfield.knime.neo4j.async;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class NumberedValue<A> {
    private final long number;
    private final A value;
    public NumberedValue(final long num, final A arg) {
        super();
        this.number = num;
        this.value = arg;
    }
    public long getNumber() {
        return number;
    }
    public A getValue() {
        return value;
    }
}
