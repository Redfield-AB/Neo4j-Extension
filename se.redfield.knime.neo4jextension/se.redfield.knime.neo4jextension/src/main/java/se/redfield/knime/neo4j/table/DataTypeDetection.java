/**
 *
 */
package se.redfield.knime.neo4j.table;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataType;
import org.knime.core.data.json.JSONCell;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class DataTypeDetection {
    private boolean isList;
    private final Set<DataType> types = new HashSet<>();

    /**
     * Default constructor.
     */
    public DataTypeDetection() {
        super();
    }

    public DataTypeDetection(final DataType t) {
        super();
        types.add(t);
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
        if (type != null) {
            types.add(type);
        }
    }
    public void addListTypes(final Collection<DataType> types) {
        this.types.addAll(types);
    }
    public Set<DataType> getTypes() {
        return types;
    }
    public boolean isDetected() {
        return !getTypes().isEmpty();
    }
    public DataType calculateType() {
        DataType curr = null;
        for (final DataType t : types) {
            if (curr == null) {
                curr = t;
            } else if (curr.getCellClass().isAssignableFrom(t.getCellClass())){
                //if new type is superclass for current type
                //should use new type
                curr = t;
            } else if (!curr.getCellClass().isAssignableFrom(t.getCellClass())) {
                //if both types is not inherited one from other
                //should use JSON type
                return JSONCell.TYPE;
            }
        }
        return curr;
    }
    public void update(final DataTypeDetection dt) {
        if (dt.isList()) {
            this.isList = true;
        }
        types.addAll(dt.getTypes());
    }
}
