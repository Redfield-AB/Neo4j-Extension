/**
 *
 */
package se.redfield.knime.neo4jextension;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JReaderModel extends NodeModel {
    private String script;

    public Neo4JReaderModel() {
        super(new PortType[] {ConnectorPortObject.TYPE},
                new PortType[] {BufferedDataTable.TYPE, ConnectorPortObject.TYPE_OPTIONAL});
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {}

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {}

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("script", script);
    }
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (!settings.containsKey("script")) {
            throw new InvalidSettingsException("Not script found");
        }
    }
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        this.script = settings.getString("script");
    }
    @Override
    protected PortObject[] execute(final PortObject[] input, final ExecutionContext exec) throws Exception {
        ConnectorPortObject neo4j = (ConnectorPortObject) input[0];
        buildOuputTable(exec, neo4j);
        return new PortObject[] {
                null,
                neo4j //forward connection
        };
    }

    /**
     * @param exec
     * @param neo4j
     * @throws CanceledExecutionException
     */
    private BufferedDataTable buildOuputTable(final ExecutionContext exec,
            final ConnectorPortObject neo4j) throws CanceledExecutionException {
        DataTable fTable = createFileTable(exec);
        BufferedDataTable table = exec.createBufferedDataTable(fTable,
                exec.createSubExecutionContext(0.0));
        return table;
    }
    /**
     * @param exec
     * @return
     */
    private DataTable createFileTable(final ExecutionContext exec) {
//        List<Record> result = neo4j.run(script);
//
//        return new DataTable() {
//            /** {@inheritDoc} */
//            @Override
//            public DataTableSpec getDataTableSpec() {
//                return ric.getDataTableSpec();
//            }
//
//            /** {@inheritDoc} */
//            @Override
//            public RowIterator iterator() {
//                return ric.iterator();
//            }
//        };
        final BufferedDataContainer container = exec.createDataContainer(
                createScriptOutputSpec());
        try {
            StringCell value = new StringCell(script);
            container.addRowToTable(new DefaultRow(RowKey.createRowKey(0l), value));
        } finally {
            container.close();
        }

        return container.getTable();
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        return new BufferedDataTable[0]; // just disable
    }
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs.length < 1) {
            throw new InvalidSettingsException("Not input found");
        }

        return new PortObjectSpec[] {
                null,
                inSpecs[0] //forward connection
        };
    }

    private DataTableSpec createScriptOutputSpec() {
        final DataColumnSpec column = new DataColumnSpecCreator(
                "string", StringCell.TYPE).createSpec();
        return new DataTableSpec(column);
    }
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] input) {
        return new DataTableSpec[0]; //just disable
    }
    @Override
    protected void reset() {
    }

    public void setScript(final String script) {
        this.script = script;
    }
    public String getScript() {
        return script;
    }
}
