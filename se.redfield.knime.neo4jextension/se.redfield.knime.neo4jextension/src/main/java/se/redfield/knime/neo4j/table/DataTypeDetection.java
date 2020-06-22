/**
 *
 */
package se.redfield.knime.neo4j.table;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataType;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class DataTypeDetection {
    private boolean isList;
    private DataType type;
    private final Set<DataType> listTypes = new HashSet<>();

    /**
     * Default constructor.
     */
    public DataTypeDetection() {
        super();
    }

    public DataTypeDetection(final DataType t) {
        super();
        this.type = t;
    }
    public DataTypeDetection(final boolean b, final List<DataType> listTypes) {
        super();
        setList(true);
        addListTypes(listTypes);
    }

    public void setList(final boolean isList) {
        this.isList = isList;
    }
    public boolean isList() {
        return isList;
    }
    public void setType(final DataType type) {
        this.type = type;
    }
    public DataType getType() {
        return type;
    }
    public void addListTypes(final Collection<DataType> types) {
        this.listTypes.addAll(types);
    }
    public Set<DataType> getListTypes() {
        return listTypes;
    }

    public boolean isDetected() {
        return getType() != null || isList() && !getListTypes().isEmpty();
    }
    public DataType getListType() {
        if (!listTypes.isEmpty()) {
            return listTypes.iterator().next();
        }
        return null;
    }
}
