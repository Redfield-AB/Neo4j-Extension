/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.Component;

import javax.swing.JSplitPane;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class SplitPanelExt extends JSplitPane {
    private static final long serialVersionUID = 8532553437433021028L;
    private final DividerLocationHolder divider = new DividerLocationHolder(this);

    public SplitPanelExt() {
        super();
    }
    public SplitPanelExt(final int newOrientation) {
        super(newOrientation);
    }
    public SplitPanelExt(final int newOrientation, final boolean newContinuousLayout) {
        super(newOrientation, newContinuousLayout);
    }
    public SplitPanelExt(final int newOrientation, final Component newLeftComponent, final Component newRightComponent) {
        super(newOrientation, newLeftComponent, newRightComponent);
    }
    public SplitPanelExt(final int newOrientation, final boolean newContinuousLayout, final Component newLeftComponent,
            final Component newRightComponent) {
        super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
    }

    @Override
    public void setDividerLocation(final double d) {
        super.setDividerLocation(getDividerLocation());
        divider.setLocation(d);
    }
    @Override
    public void setDividerLocation(final int location) {
        super.setDividerLocation(location);
        divider.setLocation(location);
    }
    @Override
    public int getDividerLocation() {
        return divider.getDividerLocation();
    }
}
