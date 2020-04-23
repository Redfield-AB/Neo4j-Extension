/**
 *
 */
package se.redfield.knime.neo4jextension.ui;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderLabel {
    public enum Type {
        NodeLabel,
        RelationshipType,
        PropertyKey
    }

    private final Type type;
    private String text;

    public ReaderLabel(final Type type) {
        this(type, null);
    }
    public ReaderLabel(final Type type, final String text) {
        super();
        this.type = type;
        this.text = text;
    }

    public void setText(final String text) {
        this.text = text;
    }
    public String getText() {
        return text;
    }
    public Type getType() {
        return type;
    }
    @Override
    public String toString() {
        return text == null ? "" : text;
    }
}
