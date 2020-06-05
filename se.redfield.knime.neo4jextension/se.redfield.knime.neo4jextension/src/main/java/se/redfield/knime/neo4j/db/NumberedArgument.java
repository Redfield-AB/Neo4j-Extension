/**
 *
 */
package se.redfield.knime.neo4j.db;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
class NumberedArgument<A> {
    private final int number;
    private final A argument;
    public NumberedArgument(final int num, final A arg) {
        super();
        this.number = num;
        this.argument = arg;
    }
    public int getNumber() {
        return number;
    }
    public A getArgument() {
        return argument;
    }
}
