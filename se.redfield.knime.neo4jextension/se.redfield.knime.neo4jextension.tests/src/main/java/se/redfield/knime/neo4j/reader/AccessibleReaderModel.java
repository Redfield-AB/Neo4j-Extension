/**
 *
 */
package se.redfield.knime.neo4j.reader;

import org.knime.core.node.context.NodeCreationConfiguration;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AccessibleReaderModel extends ReaderModel {
    public AccessibleReaderModel(final NodeCreationConfiguration creationConfig) {
        super(creationConfig);
    }

    public String getWarning() {
        return getWarningMessage();
    }
}
