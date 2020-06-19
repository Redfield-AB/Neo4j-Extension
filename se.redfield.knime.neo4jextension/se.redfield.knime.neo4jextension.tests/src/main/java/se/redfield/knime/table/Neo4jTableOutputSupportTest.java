/**
 *
 */
package se.redfield.knime.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

import junit.framework.AssertionFailedError;
import se.redfield.knime.neo4j.db.Neo4jDataConverter;
import se.redfield.knime.neo4j.db.Neo4jSupport;
import se.redfield.knime.neo4j.table.Neo4jTableOutputSupport;
import se.redfield.knime.neo4j.utils.Neo4jHelper;
import se.redfield.knime.runner.KnimeTestRunner;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
@RunWith(KnimeTestRunner.class)
public class Neo4jTableOutputSupportTest {
    private Neo4jTableOutputSupport support;
    private Driver driver;

    /**
     * Default constructor.
     */
    public Neo4jTableOutputSupportTest() {
        super();
    }

    @Before
    public void setUp() {
        driver = Neo4jHelper.createDriver();
        support = new Neo4jTableOutputSupport(new Neo4jDataConverter(driver.defaultTypeSystem()));
    }
    @After
    public void tearDown() {
        driver.close();
    }

    @Test
    public void testDatesInMap() {
        final String query = "UNWIND [\n" +
                "    { title: \"Cypher Basics I\",\n" +
                "      created: datetime(\"2019-06-01T18:40:32.142+0100\"),\n" +
                "      datePublished: date(\"2019-06-01\"),\n" +
                "      readingTime: duration({minutes: 2, seconds: 15})  },\n" +
                "    { title: \"Cypher Basics II\",\n" +
                "      created: datetime(\"2019-06-02T10:23:32.122+0100\"),\n" +
                "      datePublished: date(\"2019-06-02\"),\n" +
                "      readingTime: duration({minutes: 2, seconds: 30}) },\n" +
                "    { title: \"Dates, Datetimes, and Durations in Neo4j\",\n" +
                "      created: datetime(),\n" +
                "      datePublished: date(),\n" +
                "      readingTime: duration({minutes: 3, seconds: 30}) }\n" +
                "] AS articleProperties\n" +
                "return articleProperties";

        final List<Record> res = run(query);
        System.out.println(res);
    }
    @Test
    public void testDuration() {
        final List<Record> res = run("return datetime(\"2019-06-01T18:40:32.142+0100\") as dt");
        assertEquals(1, res.size());

        final Record rec = res.get(0);
        assertEquals(1, rec.size());

        final Value v = rec.get(0);

        //test type
        final DataType t = support.getCompatibleCellType(v);
        assertNotNull(t);

        //test value
        final DataCell cell = support.createCell(v);
        assertNotNull(cell);
    }
    @Test
    public void testDate() {
        throw new AssertionFailedError("TODO");
    }
    @Test
    public void testDateTime() {
        throw new AssertionFailedError("TODO");
    }
    @Test
    public void testCollections() {
        final String query = "UNWIND [\n" +
                "    { title: \"Cypher Basics I\",\n" +
                "      created: datetime(\"2019-06-01T18:40:32.142+0100\"),\n" +
                "      datePublished: date(\"2019-06-01\"),\n" +
                "      readingTime: duration({minutes: 2, seconds: 15})  },\n" +
                "    { title: \"Cypher Basics II\",\n" +
                "      created: datetime(\"2019-06-02T10:23:32.122+0100\"),\n" +
                "      datePublished: date(\"2019-06-02\"),\n" +
                "      readingTime: duration({minutes: 2, seconds: 30}) },\n" +
                "    { title: \"Dates, Datetimes, and Durations in Neo4j\",\n" +
                "      created: datetime(),\n" +
                "      datePublished: date(),\n" +
                "      readingTime: duration({minutes: 3, seconds: 30}) }\n" +
                "] AS t\n" +
                "return collect(t.title)";

        final List<Record> res = run(query);
        assertEquals(1, res.size());

        final Record r = res.get(0);
        assertEquals(1,  r.size());

        final Value v = r.get(0);
        final DataType t = support.getCompatibleCellType(v);
        assertEquals(ListCell.getCollectionType(StringCell.TYPE), t);
    }

    private List<Record> run(final String query) {
        return Neo4jSupport.runRead(driver, query, null);
    }
}
