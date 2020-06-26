/**
 *
 */
package se.redfield.knime.neo4j.writer;

import java.util.List;

import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.NamedWithProperties;
import se.redfield.knime.neo4j.db.LabelsAndFunctions;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class WriterConfig implements Cloneable {
    private String script;
    private String inputColumn;
    private LabelsAndFunctions metaData = new LabelsAndFunctions();
    private boolean stopOnQueryFailure;
    private boolean useAsync;
    private boolean keepSourceOrder = true;

    public WriterConfig() {
        super();
    }

    public String getScript() {
        return script;
    }
    public void setScript(final String script) {
        this.script = script;
    }
    public String getInputColumn() {
        return inputColumn;
    }
    public void setInputColumn(final String inputColumn) {
        this.inputColumn = inputColumn;
    }
    public List<NamedWithProperties> getNodeLabels() {
        return metaData.getNodes();
    }
    public List<NamedWithProperties> getRelationshipTypes() {
        return metaData.getRelationships();
    }
    public void setNodeLabels(final List<NamedWithProperties> nodeLabels) {
        metaData.setNodes(nodeLabels);
    }
    public void setRelationshipTypes(final List<NamedWithProperties> relationshipTypes) {
        metaData.setRelationships(relationshipTypes);
    }
    public void setMetaData(final LabelsAndFunctions md) {
        if (md == null) {
            throw new NullPointerException("Metadata");
        }
        this.metaData = md;
    }
    public List<FunctionDesc> getFunctions() {
        return metaData.getFunctions();
    }
    public void setFunctions(final List<FunctionDesc> functions) {
        metaData.setFunctions(functions);
    }
    public LabelsAndFunctions getMetaData() {
        return metaData;
    }
    public boolean isUseAsync() {
        return useAsync;
    }
    public void setUseAsync(final boolean useAsync) {
        this.useAsync = useAsync;
    }
    public boolean isStopOnQueryFailure() {
        return stopOnQueryFailure;
    }
    public void setStopOnQueryFailure(final boolean stop) {
        this.stopOnQueryFailure = stop;
    }
    public void setKeepSourceOrder(final boolean keepSourceOrder) {
        this.keepSourceOrder = keepSourceOrder;
    }
    public boolean isKeepSourceOrder() {
        return keepSourceOrder;
    }

    @Override
    public WriterConfig clone() {
        try {
            final WriterConfig clone = (WriterConfig) super.clone();
            clone.metaData = metaData.clone();
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
