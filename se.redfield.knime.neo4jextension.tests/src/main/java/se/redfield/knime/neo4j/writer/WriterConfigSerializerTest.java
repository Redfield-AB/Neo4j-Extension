/**
 *
 */
package se.redfield.knime.neo4j.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;

import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.NamedWithProperties;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class WriterConfigSerializerTest {
    /**
     * Default constructor.
     */
    public WriterConfigSerializerTest() {
        super();
    }

    @Test
    public void testSerialize() throws InvalidSettingsException {
        WriterConfig config = new WriterConfig();

        //functions
        final String funcName = "func-name";
        final String funcDescription = "func-desc";
        final String funcSignature = "func-sig";

        final FunctionDesc func = new FunctionDesc();
        func.setName(funcName);
        func.setDescription(funcDescription);
        func.setSignature(funcSignature);

        List<FunctionDesc> functions = new LinkedList<>();
        functions.add(func);

        //node labels
        final String labelName = "labelName";
        final String labelProp = "labelProp";

        final NamedWithProperties label = new NamedWithProperties();
        label.setName(labelName);
        label.getProperties().add(labelProp);

        List<NamedWithProperties> nodeLabels = new LinkedList<>();
        nodeLabels.add(label);

        //relationship types
        final String typeName = "typeName";
        final String typeProp = "typeProp";

        final NamedWithProperties type = new NamedWithProperties();
        type.setName(typeName);
        type.getProperties().add(typeProp);

        List<NamedWithProperties> relationshipTypes = new LinkedList<>();
        relationshipTypes.add(type);

        //other props
        final String inputColumn = "col2";
        final String script = "test-script";
        final boolean stopOnQueryFailure = true;
        final boolean useAsync = true;
        final boolean keepSourceOrder = !config.isKeepSourceOrder(); //not default value

        config.setFunctions(functions);
        config.setInputColumn(inputColumn);
        config.setNodeLabels(nodeLabels);
        config.setRelationshipTypes(relationshipTypes);
        config.setScript(script);
        config.setStopOnQueryFailure(stopOnQueryFailure);
        config.setUseAsync(useAsync);
        config.setKeepSourceOrder(keepSourceOrder);

        final NodeSettings settings = new NodeSettings("junit");

        final WriterConfigSerializer ser = new WriterConfigSerializer();
        ser.save(config, settings);
        config = ser.read(settings);

        //test correct serialized/deserialized
        functions = config.getFunctions();
        assertEquals(1, functions.size());
        assertEquals(funcName, functions.get(0).getName());
        assertEquals(funcDescription, functions.get(0).getDescription());
        assertEquals(funcSignature, functions.get(0).getSignature());

        nodeLabels = config.getNodeLabels();
        assertEquals(1, nodeLabels.size());
        assertEquals(labelName, nodeLabels.get(0).getName());
        assertEquals(1, nodeLabels.get(0).getProperties().size());
        assertTrue(nodeLabels.get(0).getProperties().contains(labelProp));

        relationshipTypes = config.getRelationshipTypes();
        assertEquals(1, relationshipTypes.size());
        assertEquals(typeName, relationshipTypes.get(0).getName());
        assertEquals(1, relationshipTypes.get(0).getProperties().size());
        assertTrue(relationshipTypes.get(0).getProperties().contains(typeProp));

        assertEquals(inputColumn, config.getInputColumn());
        assertEquals(script, config.getScript());
        assertEquals(stopOnQueryFailure, config.isStopOnQueryFailure());
        assertEquals(useAsync, config.isUseAsync());
        assertEquals(keepSourceOrder, config.isKeepSourceOrder());
    }
}
