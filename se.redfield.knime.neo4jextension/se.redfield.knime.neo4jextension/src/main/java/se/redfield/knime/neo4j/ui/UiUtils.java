/**
 *
 */
package se.redfield.knime.neo4j.ui;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class UiUtils {
    /**
     * @param script origin script.
     * @param vars flow variables map.
     * @return script with inserted flow variables.
     */
    public static String insertFlowVariables(final String script, final FlowVariablesProvider varsProvider) {
        final Map<String, FlowVariable> vars = getAvailableFlowVariables(varsProvider);
        final int[] indexes = getSortedVarOccurences(script, vars);

        final StringBuilder sb = new StringBuilder(script);
        for (int i = indexes.length - 1; i >= 0; i--) {
            final int offset = indexes[i];
            final int end = sb.indexOf("}}", offset + 2) + 2;

            final String var = sb.substring(offset + 3, end - 2);
            sb.replace(offset, end, vars.get(var).getValueAsString());
        }

        return sb.toString();
    }

    public static Map<String, FlowVariable> getAvailableFlowVariables(
            final FlowVariablesProvider varsProvider) {
        return varsProvider.getAvailableFlowVariables(getFlowVariableTypes());
    }

    /**
     * @param script script.
     * @param vars array of offsets of flow variables.
     * @return
     */
    private static int[] getSortedVarOccurences(final String script, final Map<String, FlowVariable> vars) {
        int pos = 0;
        final List<Integer> offsets = new LinkedList<Integer>();
        while (true) {
            final int offset = script.indexOf("${{", pos);
            if (offset < 0) {
                break;
            }

            final int end = script.indexOf("}}", offset);
            if (end < 0) {
                break;
            }

            //exclude non existing variables
            final String varName = script.substring(offset + 3, end);
            if (vars.containsKey(varName)) {
                offsets.add(offset);
            }
            pos = end;
        }

        //convert to int array
        final int[] result = new int[offsets.size()];
        int i = 0;
        for (final Integer o : offsets) {
            result[i] = o.intValue();
            i++;
        }
        return result;
    }

    /**
     * @return all flow variable types.
     */
    @SuppressWarnings("rawtypes")
    private static VariableType[] getFlowVariableTypes() {
        final Set<VariableType<?>> types = new HashSet<>();
        types.add(VariableType.BooleanArrayType.INSTANCE);
        types.add(VariableType.BooleanType.INSTANCE);
        types.add(VariableType.CredentialsType.INSTANCE);
        types.add(VariableType.DoubleArrayType.INSTANCE);
        types.add(VariableType.BooleanArrayType.INSTANCE);
        types.add(VariableType.DoubleArrayType.INSTANCE);
        types.add(VariableType.DoubleType.INSTANCE);
        types.add(VariableType.IntArrayType.INSTANCE);
        types.add(VariableType.IntType.INSTANCE);
        types.add(VariableType.LongArrayType.INSTANCE);
        types.add(VariableType.LongType.INSTANCE);
        types.add(VariableType.StringArrayType.INSTANCE);
        types.add(VariableType.StringType.INSTANCE);

        return types.toArray(new VariableType[types.size()]);
    }
}
