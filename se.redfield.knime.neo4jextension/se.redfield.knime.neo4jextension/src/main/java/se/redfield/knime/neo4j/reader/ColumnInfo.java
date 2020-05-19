/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.util.Objects;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ColumnInfo implements Comparable<ColumnInfo> {
    private String name;
    private int offset;

    public ColumnInfo() {
        super();
    }

    public ColumnInfo(final String name, final int index) {
        super();
        this.name = name;
        this.offset = index;
    }

    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }
    public int getOffset() {
        return offset;
    }
    public void setOffset(final int offset) {
        this.offset = offset;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ColumnInfo)) {
            return false;
        }

        final ColumnInfo that = (ColumnInfo) obj;
        return Objects.equals(name, that.name)
                && this.offset == that.offset;
    }
    @Override
    public int hashCode() {
        return Objects.hash(name, offset);
    }
    @Override
    public int compareTo(final ColumnInfo o) {
        return Integer.compare(this.offset, o.offset);
    }
    @Override
    public String toString() {
        return String.valueOf(name);
    }
}
