/**
 *
 */
package se.redfield.knime.neo4j.reader.async;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class NumberedString {
    private final int number;
    private final String string;
    public NumberedString(final int num, final String string) {
        super();
        this.number = num;
        this.string = string;
    }
    public int getNumber() {
        return number;
    }
    public String getString() {
        return string;
    }
}
