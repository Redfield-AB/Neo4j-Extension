/**
 *
 */
package se.redfield.knime.neo4j.reader.cfg;

import se.redfield.knime.neo4j.reader.ColumnInfo;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderConfig implements Cloneable {
    private String script;
    private boolean useJson = true; //default true
    private ColumnInfo inputColumn;

    public ReaderConfig() {
        super();
    }

    public String getScript() {
        return script;
    }
    public void setScript(final String script) {
        this.script = script;
    }
    public boolean isUseJson() {
        return useJson;
    }
    public void setUseJson(final boolean useJson) {
        this.useJson = useJson;
    }
    public ColumnInfo getInputColumn() {
        return inputColumn;
    }
    public void setInputColumn(final ColumnInfo inputColumn) {
        this.inputColumn = inputColumn;
    }
    @Override
    public ReaderConfig clone() {
        try {
            final ReaderConfig clone = (ReaderConfig) super.clone();
            if (inputColumn != null) {
                clone.inputColumn = new ColumnInfo(inputColumn.getName(), inputColumn.getOffset());
            }
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
