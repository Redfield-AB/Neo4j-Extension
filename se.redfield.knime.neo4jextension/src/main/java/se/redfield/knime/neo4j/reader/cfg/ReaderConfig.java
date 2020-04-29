/**
 *
 */
package se.redfield.knime.neo4j.reader.cfg;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderConfig {
    private String script;
    private boolean useJson = true; //default true

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
}
