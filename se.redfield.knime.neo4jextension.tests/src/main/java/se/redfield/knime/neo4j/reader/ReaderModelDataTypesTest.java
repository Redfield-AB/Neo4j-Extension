/**
 *
 */
package se.redfield.knime.neo4j.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.redfield.knime.neo4j.utils.KNimeHelper.createConnectorPortObject;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.port.PortObject;

import se.redfield.knime.neo4j.utils.KNimeHelper;
import se.redfield.knime.runner.KnimeTestRunner;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@RunWith(KnimeTestRunner.class)
public class ReaderModelDataTypesTest {
    private ReaderModel model;

    /**
     * Default constructor.
     */
    public ReaderModelDataTypesTest() {
        super();
    }

    @Before
    public void setUp() {
        final ModifiableNodeCreationConfiguration cfg = new ReaderFactory().createNodeCreationConfig();
        model = new ReaderModel(cfg);
    }

    @Test
    public void testConfigureUsingScriptTableOutput() throws Exception {
        setScript("return 1");

        final BufferedDataTable out = execute();

        final List<DataRow> rows = getAllRows(out);
        assertEquals(1, rows.size());

        final DataRow row = rows.get(0);
        assertEquals(1, rows.size());

        final DataCell cell = row.getCell(0);
        assertTrue(cell instanceof LongCell);
        assertEquals(1l, ((LongCell) cell).getLongValue());
    }
    @Test
    public void testCollection() throws Exception {
        final String query = "UNWIND [\n" +
                "    { title: \"Cypher Basics I\"},\n" +
                "    { title: \"Cypher Basics II\"},\n" +
                "    { title: \"Dates, Datetimes, and Durations in Neo4j\"}\n" +
                "] AS t\n" +
                "return collect(t.title)";
        setScript(query);

        final BufferedDataTable out = execute();

        final List<DataRow> rows = getAllRows(out);
        assertEquals(1, rows.size());

        final DataRow row = rows.get(0);
        assertEquals(1, rows.size());

        final ListCell cell = (ListCell) row.getCell(0);
        assertEquals(StringCell.TYPE, cell.getElementType());

        assertEquals("Cypher Basics I", ((StringCell) cell.get(0)).getStringValue());
        assertEquals("Cypher Basics II", ((StringCell) cell.get(1)).getStringValue());
        assertEquals("Dates, Datetimes, and Durations in Neo4j", ((StringCell) cell.get(2)).getStringValue());
    }
    @Test
    public void testCollectionWithNulls() throws Exception {
        final String query = "return [null, null, \"value\"] as list";
        setScript(query);

        final BufferedDataTable out = execute();

        final List<DataRow> rows = getAllRows(out);
        assertEquals(1, rows.size());

        final DataRow row = rows.get(0);
        assertEquals(1, rows.size());

        final ListCell cell = (ListCell) row.getCell(0);
        assertEquals(StringCell.TYPE, cell.getElementType());

        assertTrue(cell.get(0) instanceof MissingCell);
        assertTrue(cell.get(1) instanceof MissingCell);
        assertEquals("value", ((StringCell) cell.get(2)).getStringValue());
    }
    @Test
    public void testCollectionOnlyNulls() throws Exception {
        final String query = "return [null, null, null] as list";
        setScript(query);

        final BufferedDataTable out = execute();

        final List<DataRow> rows = getAllRows(out);
        assertEquals(1, rows.size());

        final DataRow row = rows.get(0);
        assertEquals(1, rows.size());

        final ListCell cell = (ListCell) row.getCell(0);

        assertTrue(cell.get(0) instanceof MissingCell);
        assertTrue(cell.get(1) instanceof MissingCell);
        assertTrue(cell.get(2) instanceof MissingCell);
    }
    @Test
    public void testCollectionCombinedTypes() throws Exception {
        final String query = "return [1, \"string\", null] as list";
        setScript(query);

        final BufferedDataTable out = execute();

        final List<DataRow> rows = getAllRows(out);
        assertEquals(1, rows.size());

        final DataRow row = rows.get(0);
        assertEquals(1, rows.size());

        final ListCell cell = (ListCell) row.getCell(0);

        assertTrue(cell.get(0) instanceof JSONCell);
        assertTrue(cell.get(1) instanceof JSONCell);
        assertTrue(cell.get(2) instanceof MissingCell);
    }
    @Test
    public void testMultipleRows() throws Exception {
        final StringBuilder query = new StringBuilder();
        query.append("UNWIND [1, 2, 'val', NULL] AS x\n");
        query.append("RETURN x AS y");
        setScript(query.toString());

        final BufferedDataTable out = execute();

        final List<DataRow> rows = getAllRows(out);
        assertEquals(4, rows.size());

        final DataRow row = rows.get(0);
        assertEquals(1, row.getNumCells());

        assertTrue(rows.get(0).getCell(0) instanceof JSONCell);
        assertTrue(rows.get(1).getCell(0) instanceof JSONCell);
        assertTrue(rows.get(2).getCell(0) instanceof JSONCell);
        assertTrue(rows.get(3).getCell(0) instanceof MissingCell);
    }
    @Test
    public void testListMultipleRows() throws Exception {
        final StringBuilder query = new StringBuilder();
        query.append("UNWIND [[null, null, null], [1, 2, 'val1'], ['val1', 2, NULL]] AS x\n");
        query.append("RETURN x AS y");
        setScript(query.toString());

        final BufferedDataTable out = execute();

        final List<DataRow> rows = getAllRows(out);
        assertEquals(3, rows.size());

        final DataRow row = rows.get(2);
        assertEquals(1, row.getNumCells());

        final ListCell list = (ListCell) row.getCell(0);

        assertTrue(list.get(0) instanceof JSONCell);
        assertTrue(list.get(1) instanceof JSONCell);
        assertTrue(list.get(2) instanceof MissingCell);
    }

    /**
     * @return execution result
     * @throws Exception
     */
    private BufferedDataTable execute() throws Exception {
        final PortObject[] input = {null, createConnectorPortObject()};
        final PortObject[] out = model.execute(input, KNimeHelper.createExecutionContext(model));
        return (BufferedDataTable) out[0];
    }
    /**
     * @param script
     */
    private void setScript(final String script) {
        final ReaderConfig cfg = new ReaderConfig();
        cfg.setScript(script);
        cfg.setUseJson(false);

        final NodeSettings s = new NodeSettings("junit");
        new ReaderConfigSerializer().save(cfg, s);

        try {
            model.loadValidatedSettingsFrom(s);
        } catch (final InvalidSettingsException e) {
            throw new RuntimeException(e);
        }
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
}
