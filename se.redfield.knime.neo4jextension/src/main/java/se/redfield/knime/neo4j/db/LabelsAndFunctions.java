/**
 *
 */
package se.redfield.knime.neo4j.db;

import java.util.LinkedList;
import java.util.List;

import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.NamedWithProperties;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class LabelsAndFunctions implements Cloneable {
    private List<NamedWithProperties> nodes = new LinkedList<>();
    private List<NamedWithProperties> relationships = new LinkedList<>();
    private List<FunctionDesc> functions = new LinkedList<>();

    public LabelsAndFunctions() {
        super();
    }

    public List<NamedWithProperties> getNodes() {
        return nodes;
    }
    public List<NamedWithProperties> getRelationships() {
        return relationships;
    }
    public List<FunctionDesc> getFunctions() {
        return functions;
    }
    public void setNodes(final List<NamedWithProperties> nodes) {
        this.nodes = nodes;
    }
    public void setRelationships(final List<NamedWithProperties> relationships) {
        this.relationships = relationships;
    }
    public void setFunctions(final List<FunctionDesc> functions) {
        this.functions = functions;
    }
    @Override
    public LabelsAndFunctions clone() {
        try {
            final LabelsAndFunctions clone = (LabelsAndFunctions) super.clone();
            clone.functions = cloneFunctions(functions);
            clone.nodes = cloneNamed(nodes);
            clone.relationships = cloneNamed(relationships);
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
    private List<NamedWithProperties> cloneNamed(final List<NamedWithProperties> nameds) {
        final List<NamedWithProperties> list = new LinkedList<>();
        for (final NamedWithProperties n : nameds) {
            list.add(n.clone());
        }
        return list;
    }
    private List<FunctionDesc> cloneFunctions(final List<FunctionDesc> f) {
        final List<FunctionDesc> list = new LinkedList<>();
        for (final FunctionDesc desc : f) {
            list.add(desc.clone());
        }
        return list;
    }

    public boolean isEmpty() {
        return nodes.isEmpty() && relationships.isEmpty() && functions.isEmpty();
    }
}
