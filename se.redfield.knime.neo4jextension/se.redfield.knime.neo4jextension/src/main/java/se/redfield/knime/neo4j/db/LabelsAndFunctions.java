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
public class LabelsAndFunctions {
    private final List<NamedWithProperties> nodes = new LinkedList<>();
    private final List<NamedWithProperties> relationships = new LinkedList<>();
    private final List<FunctionDesc> functions = new LinkedList<>();

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
}
