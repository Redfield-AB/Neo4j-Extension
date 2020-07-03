/**
 *
 */
package se.redfield.knime.neo4j.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;

import junit.framework.AssertionFailedError;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.utils.KNimeHelper;
import se.redfield.knime.neo4j.utils.Neo4jHelper;
import se.redfield.knime.neo4j.utils.TestRowInput;
import se.redfield.knime.neo4j.utils.TestRowOutput;
import se.redfield.knime.runner.KnimeTestRunner;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@RunWith(KnimeTestRunner.class)
public class ReaderModelStreamableTest {
    private AccessibleReaderModel model;

    /**
     * Default constructor.
     */
    public ReaderModelStreamableTest() {
        super();
    }

    @Before
    public void setUp() {
        final ModifiableNodeCreationConfiguration cfg = new ReaderFactory().createNodeCreationConfig();
        model = new AccessibleReaderModel(cfg);
    }

    @Test
    public void testRunUsingScript() throws Exception {
        final ReaderConfig cfg = new ReaderConfig();
        cfg.setScript("return 'string output'");
        cfg.setUseJson(false);

        setConfigToModel(cfg);

        final PortObject[] input = {null, createConnectorPortObject()};
        try {
            execute(input);
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            // Ok
        }
    }
    @Test
    public void testRunUsingTableInput() throws Exception {
        final String columnName = "input";

        final ReaderConfig cfg = new ReaderConfig();
        cfg.setInputColumn(columnName);
        //set script, but it will ignored
        cfg.setScript("return 'string output'");
        //set as not use JSON but it will ignored
        cfg.setUseJson(false);

        setConfigToModel(cfg);

        final List<String> scripts = new LinkedList<String>();
        scripts.add("return 'string from table input'");
        final PortObject[] input = {
                createTable(scripts, columnName),
                createConnectorPortObject()};
        final PortObject[] out = execute(input);

        assertTrue(out[1] instanceof ConnectorPortObject);
        assertTrue(out[0] instanceof BufferedDataTable);

        final BufferedDataTable table = (BufferedDataTable) out[0];
        assertEquals(2, table.getSpec().getNumColumns());
        assertEquals(StringCell.TYPE, table.getSpec().getColumnSpec(0).getType());
        assertEquals(JSONCell.TYPE, table.getSpec().getColumnSpec(1).getType());
        assertEquals(1, table.size());

        final List<DataRow> rows = getAllRows(table);
        assertEquals(1, rows.size());

        final DataRow row = rows.get(0);
        assertEquals(2, row.getNumCells());

        assertEquals(StringCell.TYPE, row.getCell(0).getType());
        final DataCell cell = row.getCell(1);
        assertEquals(JSONCell.TYPE, cell.getType());

        final JSONCell json = (JSONCell) cell;
        assertTrue(json.getStringValue().contains("string from table input"));
    }
    @Test
    public void testTableInputEmptyTable() throws Exception {
        final String columnName = "input";

        final ReaderConfig cfg = new ReaderConfig();
        cfg.setInputColumn(columnName);
        setConfigToModel(cfg);

        final List<String> scripts = new LinkedList<String>();
        final PortObject[] input = {
                createTable(scripts, columnName),
                createConnectorPortObject()};
        final PortObject[] out = execute(input);

        assertTrue(out[1] instanceof ConnectorPortObject);
        assertTrue(out[0] instanceof BufferedDataTable);

        final BufferedDataTable table = (BufferedDataTable) out[0];

        assertEquals(2, table.getSpec().getNumColumns());
        assertEquals(StringCell.TYPE, table.getSpec().getColumnSpec(0).getType());
        assertEquals(JSONCell.TYPE, table.getSpec().getColumnSpec(1).getType());

        assertEquals(0, table.size());
    }
    @Test
    public void testOutputRowsOrderUsingTableInput() throws Exception {
        final String columnName = "input";

        final ReaderConfig cfg = new ReaderConfig();
        cfg.setInputColumn(columnName);

        setConfigToModel(cfg);

        final int max = 1008;
        final String prefix = "script-";
        final List<String> scripts = new LinkedList<String>();
        for (int i = 0; i < max; i++) {
            scripts.add("return '" + prefix + i + "'");
        }

        final PortObject[] input = {
                createTable(scripts, columnName),
                createConnectorPortObject()};
        final PortObject[] out = execute(input);

        final BufferedDataTable table = (BufferedDataTable) out[0];

        final List<DataRow> rows = getAllRows(table);
        assertEquals(max, rows.size());
        int i = 0;
        for (final DataRow row : rows) {
            final JSONCell cell = (JSONCell) row.getCell(1);
            assertTrue(cell.getStringValue().contains(prefix + i));
            i++;
        }
    }
    @Test
    public void testRunUsingTableStopWithQueryFailure() throws Exception {
        final String columnName = "input";

        final ReaderConfig cfg = new ReaderConfig();
        cfg.setInputColumn(columnName);

        final List<String> scripts = new LinkedList<>();
        scripts.add("abrakadabra1");
        scripts.add("abrakadabra2");

        final PortObject[] input = {
                createTable(scripts, columnName),
                createConnectorPortObject()};

        cfg.setStopOnQueryFailure(false);
        setConfigToModel(cfg);
        //should not throw exception
        execute(input);
        assertEquals(ReaderModel.SOME_QUERIES_ERROR, model.getWarning());

        cfg.setStopOnQueryFailure(true);
        setConfigToModel(cfg);
        try {
            execute(input);
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            // OK
        }
    }
    /**
     * @param input
     * @return
     * @throws Exception
     */
    private PortObject[] execute(final PortObject[] inPorts) throws Exception {
        //create inputs
        PortObjectSpec[] inSpecs;
        PortInput[] inputs;

        if (inPorts.length < 2) {
            inSpecs = new PortObjectSpec[] {inPorts[0].getSpec()};
            inputs = new PortInput[] {new PortObjectInput(inPorts[0])};
        } else {
            DataTableSpec tableSpec = null;
            BufferedDataTable table = null;
            if (inPorts[0] != null) {
                table = ((BufferedDataTable) inPorts[0]);
                tableSpec = table.getSpec();
            }

            inSpecs = new PortObjectSpec[] {tableSpec, inPorts[1].getSpec()};
            inputs = new PortInput[] {tableSpec == null ?
                    null : new TestRowInput(table), new PortObjectInput(inPorts[1])};
        }

        //create inputs and outputs
        final ExecutionContext exec = KNimeHelper.createExecutionContext(model);
        final PortObjectSpec[] outputSpecs = model.configure(inSpecs);
        final BufferedDataContainer table = exec.createDataContainer(
                (DataTableSpec) outputSpecs[0]);

        final PortOutput[] outputs = {
                new TestRowOutput(table),
                new PortObjectOutput()
        };

        //execute
        try {
            final StreamableFunction op = model.createStreamableOperator(null, inSpecs);
            op.runFinal(inputs, outputs, exec);
        } finally {
            table.close();
        }

        //create result
        return new PortObject[] {
                table.getTable(),
                ((PortObjectOutput) outputs[1]).getPortObject()};
    }
    /**
     * @param table buffered data table.
     * @return all rows from given table.
     */
    private List<DataRow> getAllRows(final BufferedDataTable table) {
        final List<DataRow> rows = new LinkedList<>();
        for (final DataRow dataRow : table) {
            rows.add(dataRow);
        }
        return rows;
    }
    /**
     * @param scripts list of script to place to table column.
     * @param columnName column name.
     * @return data table with one column.
     */
    private BufferedDataTable createTable(final List<String> scripts, final String columnName) {
        final DataTableSpec tableSpec = createTableSpec(columnName);

        final ExecutionContext exec = KNimeHelper.createExecutionContext(model);
        final BufferedDataContainer table = exec.createDataContainer(tableSpec);
        try {
            int i = 0;
            for (final String script : scripts) {
                final DataRow row = new DefaultRow(new RowKey("row-" + i),
                        new StringCell(script));
                table.addRowToTable(row);
                i++;
            }
        } finally {
            table.close();
        }
        return table.getTable();
    }
    /**
     * @param columnName column name.
     * @return
     */
    private DataTableSpec createTableSpec(final String columnName) {
        final DataColumnSpec column = new DataColumnSpecCreator(
                columnName, StringCell.TYPE).createSpec();
        return new DataTableSpec("inputTable", column);
    }
    /**
     * @return connector port object.
     */
    private ConnectorPortObject createConnectorPortObject() {
        return new ConnectorPortObject(Neo4jHelper.createConfig());
    }
    private void setConfigToModel(final ReaderConfig cfg) throws InvalidSettingsException {
        final NodeSettings s = new NodeSettings("junit");
        new ReaderConfigSerializer().save(cfg, s);

        model.loadValidatedSettingsFrom(s);
    }
}
