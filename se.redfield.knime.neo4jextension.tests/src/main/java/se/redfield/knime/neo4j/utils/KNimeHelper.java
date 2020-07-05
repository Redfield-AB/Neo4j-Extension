/**
 *
 */
package se.redfield.knime.neo4j.utils;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeView;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.util.ProgressMonitorAdapter;
import org.w3c.dom.Element;

import se.redfield.knime.neo4j.connector.ConnectorPortObject;
import se.redfield.knime.neo4j.connector.ConnectorSpec;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public final class KNimeHelper {
    /**
     * Default constructor.
     */
    private KNimeHelper() {
        super();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Node createNode(final NodeModel model) {
        final NodeFactory m = new NodeFactory<NodeModel>(false) {
            @Override
            protected NodeDialogPane createNodeDialogPane() {
                return null;
            }
            @Override
            public NodeModel createNodeModel() {
                return model;
            }
            @Override
            public NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
                return null;
            };
            @Override
            protected int getNrNodeViews() {
                return 0;
            }
            @Override
            protected boolean hasDialog() {
                return false;
            }
            @Override
            protected NodeDescription createNodeDescription() {
                return createSimpleNodeDescription(model);
            }
        };
        return new Node(m);
    }
    protected static NodeDescription createSimpleNodeDescription(final NodeModel model) {
        return new NodeDescription() {
            @Override
            public Element getXMLDescription() {
                return null;
            }

            @Override
            public String getViewName(final int index) {
                return null;
            }
            @Override
            public String getViewDescription(final int index) {
                return null;
            }
            @Override
            public int getViewCount() {
                return 0;
            }
            @Override
            public NodeType getType() {
                return NodeType.Other;
            }
            @Override
            public String getOutportName(final int index) {
                return "out-" + index;
            }
            @Override
            public String getOutportDescription(final int index) {
                return getOutportName(index);
            }
            @Override
            public String getNodeName() {
                return model.getClass().getSimpleName();
            }
            @Override
            public String getInteractiveViewName() {
                return null;
            }
            @Override
            public String getInportName(final int index) {
                return "in-" + index;
            }
            @Override
            public String getInportDescription(final int index) {
                return getInportName(index);
            }
            @Override
            public String getIconPath() {
                return null;
            }
        };
    }
    /**
     * @return execution context.
     */
    public static ExecutionContext createExecutionContext(final NodeModel model) {
        return new ExecutionContext(createProgressMonitor(), createNode(model),
                MemoryPolicy.CacheInMemory, NotInWorkflowDataRepository.newInstance());
    }
    /**
     * @return node progress monitor.
     */
    public static NodeProgressMonitor createProgressMonitor() {
        return new ProgressMonitorAdapter(new NullProgressMonitor());
    }
    /**
     * @return connector port specification.
     */
    public static ConnectorSpec createConnectorSpec() {
        return new ConnectorSpec(Neo4jHelper.createConfig());
    }
    /**
     * @return connector port object.
     */
    public static ConnectorPortObject createConnectorPortObject() {
        return new ConnectorPortObject(Neo4jHelper.createConfig());
    }
}
