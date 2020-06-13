/**
 *
 */
package se.redfield.knime.neo4j.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import junit.framework.AssertionFailedError;
import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.ConnectorSpec;
import se.redfield.knime.neo4j.utils.KNimeHelper;
import se.redfield.knime.neo4j.utils.Neo4jHelper;
import se.redfield.knime.runner.KnimeTestRunner;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@RunWith(KnimeTestRunner.class)
public class WriterModelTest {
    private AccessibleWriterModel model;

    /**
     * Default constructor.
     */
    public WriterModelTest() {
        super();
    }

    @Before
    public void setUp() {
        model = new AccessibleWriterModel();
    }
    @After
    public void tearDown() {
        //delete all nodes with test marker
        Neo4jHelper.write("MATCH (n { junittest: true })\nDETACH DELETE n");
    }

    @Test
    public void testConfigureWithInputTable() throws InvalidSettingsException {
        final int numColumns = 2;

        final ConnectorSpec connectorSpec = createConnectorSpec();
        final DataTableSpec inputTableSpec = createTableSpec(numColumns);
        final PortObjectSpec[] inSpecs = {
            inputTableSpec,
            connectorSpec
        };

        //run configure method
        final PortObjectSpec[] output = model.configure(inSpecs);

        //test connector output port
        assertEquals(connectorSpec, output[1]);

        //test table output port
        assertTrue(output[0] instanceof DataTableSpec);

        final DataTableSpec table = (DataTableSpec) output[0];
        assertEquals(numColumns + 1, table.getNumColumns());

        //test column specifications
        assertEqualsColumnSpecs(inputTableSpec.getColumnSpec(0), table.getColumnSpec(0));
        assertEqualsColumnSpecs(inputTableSpec.getColumnSpec(1), table.getColumnSpec(1));

        //test JSON colung
        final DataColumnSpec jsonColumn = table.getColumnSpec(2);
        assertNotNull(jsonColumn.getName());
        assertEquals(JSONCell.TYPE, jsonColumn.getType());
    }
    @Test
    public void testConfigureWithoutInputTable() throws InvalidSettingsException {
        final ConnectorSpec connectorSpec = createConnectorSpec();
        final PortObjectSpec[] inSpecs = {
            null,
            connectorSpec
        };

        //run configure method
        final PortObjectSpec[] output = model.configure(inSpecs);

        //test connector output port
        assertEquals(connectorSpec, output[1]);

        //test table output port
        assertTrue(output[0] instanceof DataTableSpec);

        final DataTableSpec table = (DataTableSpec) output[0];
        assertEquals(1, table.getNumColumns());

        //test JSON colung
        final DataColumnSpec jsonColumn = table.getColumnSpec(0);
        assertNotNull(jsonColumn.getName());
        assertEquals(JSONCell.TYPE, jsonColumn.getType());
    }
    @Test
    public void testScript() throws Exception {
        final String script = getCreateNode("n1");
        final WriterConfig cfg = new WriterConfig();
        cfg.setScript(script);

        setConfigToModel(cfg);

        final ConnectorPortObject connector = createConnectorPortObject();
        final PortObject[] input = {null, connector};
        final PortObject[] out = model.execute(input, createExecutionContext());

        assertEquals(connector, out[1]);
        assertEquals(1, Neo4jHelper.read("MATCH (n:n1)\nRETURN n").size());

        //test result table
        assertTrue(out[0] instanceof DataTable);

        final DataTable t = (DataTable) out[0];
        final List<DataRow> rows = read(t);
        assertEquals(1, rows.size());

        final DataRow row = rows.get(0);
        assertEquals(1, rows.size());

        assertTrue(row.getCell(0) instanceof JSONCell);
    }
    @Test
    public void testScriptInputByStopOnQueryFailure() throws Exception {
        final String script = "abrakadabra";
        final WriterConfig cfg = new WriterConfig();
        cfg.setScript(script);
        cfg.setStopOnQueryFailure(true);

        setConfigToModel(cfg);

        final PortObject[] input = {null, createConnectorPortObject()};
        try {
            model.execute(input, createExecutionContext());
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            // OK
        }
    }
    @Test
    public void testScriptInputNotStopOnQueryFailure() throws Exception {
        final String script = "abrakadabra";
        final WriterConfig cfg = new WriterConfig();
        cfg.setScript(script);
        cfg.setStopOnQueryFailure(false);

        setConfigToModel(cfg);

        final ConnectorPortObject connector = createConnectorPortObject();
        final PortObject[] input = {null, connector};
        //should not throw exception
        final PortObject[] out = model.execute(input, createExecutionContext());

        assertEquals(connector, out[1]);
        assertTrue(out[0] instanceof DataTable);

        final DataTable t = (DataTable) out[0];
        final List<DataRow> rows = read(t);
        assertEquals(1, rows.size());

        final DataRow row = rows.get(0);
        assertEquals(1, rows.size());

        final JSONCell cell = (JSONCell) row.getCell(0);
        final JsonObject json = cell.getJsonValue().asJsonObject();

        assertEquals("error", json.getString("status"));
        assertTrue(json.containsKey("description"));
    }
    /**
     * Tests row order in case of asynchronous script launching
     * @throws Exception
     */
    @Test
    public void testTableInputAsyncRowOrder() throws Exception {
        final String columnName = "in";
        final int maxRows = 108;

        final String tpl = "RETURN '???'";
        //create list of scripts
        final List<String> scripts = new LinkedList<>();
        for (int i = 0; i < maxRows; i++) {
            final String label = "lbl" + i;
            scripts.add(tpl.replace("???", label));
        }

        final WriterConfig cfg = new WriterConfig();
        cfg.setUseAsync(true);
        cfg.setInputColumn(columnName);

        setConfigToModel(cfg);

        final ConnectorPortObject connector = createConnectorPortObject();
        final PortObject[] input = {
            createTable(scripts, columnName),
            connector
        };
        final PortObject[] out = model.execute(input, createExecutionContext());

        assertEquals(connector, out[1]);

        //test result table
        final List<DataRow> rows = read((DataTable) out[0]);
        assertEquals(maxRows, rows.size());

        int i = 0;
        for (final DataRow dataRow : rows) {
            //two columns. First column is input second is output.
            assertEquals(2, dataRow.getNumCells());
            final JSONCell cell = (JSONCell) dataRow.getCell(1);
            cell.getStringValue().contains("lbl" + i);

            i++;
        }
    }
    @Test
    public void testTableInputSync() throws Exception {
        final String columnName = "in";
        final int maxRows = 108;

        final String tpl = "RETURN '???'";
        //create list of scripts
        final List<String> scripts = new LinkedList<>();
        for (int i = 0; i < maxRows; i++) {
            final String label = "lbl" + i;
            scripts.add(tpl.replace("???", label));
        }

        final WriterConfig cfg = new WriterConfig();
        cfg.setUseAsync(false);
        cfg.setInputColumn(columnName);

        setConfigToModel(cfg);

        final ConnectorPortObject connector = createConnectorPortObject();
        final PortObject[] input = {
            createTable(scripts, columnName),
            connector
        };
        final PortObject[] out = model.execute(input, createExecutionContext());

        assertEquals(connector, out[1]);

        //test result table
        final List<DataRow> rows = read((DataTable) out[0]);
        assertEquals(maxRows, rows.size());

        int i = 0;
        for (final DataRow dataRow : rows) {
            //two columns. First column is input second is output.
            assertEquals(2, dataRow.getNumCells());
            final JSONCell cell = (JSONCell) dataRow.getCell(1);
            cell.getStringValue().contains("lbl" + i);

            i++;
        }
    }
    @Test
    public void testTableInputByStopOnQueryFailureRunSync() throws InvalidSettingsException {
        final String columnName = "in";

        //create list of scripts
        final List<String> scripts = new LinkedList<>();
        scripts.add("abrakadabra");

        final WriterConfig cfg = new WriterConfig();
        cfg.setUseAsync(false);
        cfg.setInputColumn(columnName);
        cfg.setStopOnQueryFailure(true);

        setConfigToModel(cfg);

        final ConnectorPortObject connector = createConnectorPortObject();
        final PortObject[] input = {
            createTable(scripts, columnName),
            connector
        };
        try {
            model.execute(input, createExecutionContext());
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            // correct
        }
    }
    @Test
    public void testTableInputNotStopOnQueryFailureRunSync() throws Exception {
        final String columnName = "in";

        //create list of scripts
        final List<String> scripts = new LinkedList<>();
        scripts.add("abrakadabra");
        scripts.add("return 1");

        final WriterConfig cfg = new WriterConfig();
        cfg.setUseAsync(false);
        cfg.setInputColumn(columnName);
        cfg.setStopOnQueryFailure(false);

        setConfigToModel(cfg);

        final ConnectorPortObject connector = createConnectorPortObject();
        final PortObject[] input = {
            createTable(scripts, columnName),
            connector
        };

        final PortObject[] out = model.execute(input, createExecutionContext());

        //test result table
        final List<DataRow> rows = read((DataTable) out[0]);
        assertTrue(((JSONCell) rows.get(0).getCell(1)).getStringValue().contains("error"));
        assertTrue(((JSONCell) rows.get(1).getCell(1)).getStringValue().contains("success"));
    }
    @Test
    public void testTableInputByStopOnQueryFailureRunAsync() throws InvalidSettingsException {
        final String columnName = "in";

        //create list of scripts
        final List<String> scripts = new LinkedList<>();
        scripts.add("abrakadabra");

        final WriterConfig cfg = new WriterConfig();
        cfg.setUseAsync(true);
        cfg.setInputColumn(columnName);
        cfg.setStopOnQueryFailure(true);

        setConfigToModel(cfg);

        final ConnectorPortObject connector = createConnectorPortObject();
        final PortObject[] input = {
            createTable(scripts, columnName),
            connector
        };
        try {
            model.execute(input, createExecutionContext());
            throw new AssertionFailedError("Exception should be thrown");
        } catch (final Exception e) {
            // correct
        }
    }
    @Test
    public void testTableInputNotStopOnQueryFailureRunAsync() throws Exception {
        final String columnName = "in";

        //create list of scripts
        final List<String> scripts = new LinkedList<>();
        scripts.add("abrakadabra");
        scripts.add("return 1");

        final WriterConfig cfg = new WriterConfig();
        cfg.setUseAsync(false);
        cfg.setInputColumn(columnName);
        cfg.setStopOnQueryFailure(false);

        setConfigToModel(cfg);

        final ConnectorPortObject connector = createConnectorPortObject();
        final PortObject[] input = {
            createTable(scripts, columnName),
            connector
        };

        final PortObject[] out = model.execute(input, createExecutionContext());

        //test result table
        final List<DataRow> rows = read((DataTable) out[0]);
        assertTrue(((JSONCell) rows.get(0).getCell(1)).getStringValue().contains("error"));
        assertTrue(((JSONCell) rows.get(1).getCell(1)).getStringValue().contains("success"));
    }
    /**
     * @param spec1 first specification.
     * @param spec2 second  specification.
     */
    private void assertEqualsColumnSpecs(final DataColumnSpec spec1, final DataColumnSpec spec2) {
        assertEquals(spec1.getName(), spec2.getName());
        assertEquals(spec1.getType(), spec2.getType());
    }
    /**
     * @param numColumns number of columns.
     * @return
     */
    private DataTableSpec createTableSpec(final int numColumns) {
        final DataColumnSpec[] columns = new DataColumnSpec[numColumns];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = new DataColumnSpecCreator("col-" + i, StringCell.TYPE).createSpec();
        }
        return new DataTableSpec(columns);
    }
    /**
     * @return connector port specification.
     */
    private ConnectorSpec createConnectorSpec() {
        return new ConnectorSpec(Neo4jHelper.createConfig());
    }
    /**
     * @return connector port object.
     */
    private ConnectorPortObject createConnectorPortObject() {
        return new ConnectorPortObject(Neo4jHelper.createConfig());
    }
    /**
     * @param label node label.
     * @return script for create node and marked by 'junitest' attribute for found
     * and cleanup this node after test finished.
     */
    private String getCreateNode(final String label) {
        return "CREATE (n:" + label + " {junittest: true})";
    }
    private void setConfigToModel(final WriterConfig cfg) throws InvalidSettingsException {
        final NodeSettings s = new NodeSettings("junit");
        new WriterConfigSerializer().save(cfg, s);

        model.loadValidatedSettingsFrom(s);
    }
    /**
     * @param scripts list of script to place to table column.
     * @param columnName column name.
     * @return data table with one column.
     */
    private BufferedDataTable createTable(final List<String> scripts, final String columnName) {
        final DataColumnSpec column = new DataColumnSpecCreator(
                columnName, StringCell.TYPE).createSpec();
        final DataTableSpec tableSpec = new DataTableSpec("inputTable", column);

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
    private ExecutionContext createExecutionContext() {
        return KNimeHelper.createExecutionContext(model);
    }
    private List<DataRow> read(final DataTable t) {
        final List<DataRow> rows = new LinkedList<>();
        for (final DataRow row : t) {
            rows.add(row);
        }
        return rows;
    }
}
