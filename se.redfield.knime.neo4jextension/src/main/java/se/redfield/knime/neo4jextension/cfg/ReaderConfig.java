/**
 *
 */
package se.redfield.knime.neo4jextension.cfg;

import java.util.LinkedList;
import java.util.List;

import se.redfield.knime.neo4jextension.SourceType;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class ReaderConfig {
    private String script;
    private boolean useJson = true; //default true
    private SourceType source = SourceType.Script; //default script
    private List<String> nodeLabels = new LinkedList<>();
    private List<String> relationshipTypes = new LinkedList<>();

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
    public SourceType getSource() {
        return source;
    }
    public void setSource(final SourceType source) {
        this.source = source;
    }
    public List<String> getNodeLabels() {
        return nodeLabels;
    }
    public void setNodeLabels(final List<String> nodeLabels) {
        this.nodeLabels = nodeLabels;
    }
    public List<String> getRelationshipTypes() {
        return relationshipTypes;
    }
    public void setRelationshipTypes(final List<String> relationshipTypes) {
        this.relationshipTypes = relationshipTypes;
    }
}
