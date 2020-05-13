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
    private double divider = 0.;

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
        if (d < 0.0 || d > 1.0) {
            throw new IllegalArgumentException("proportional location must "
                    + "be between 0.0 and 1.0.");
        }

        divider = d;
        super.setDividerLocation(getDividerLocation());
    }
    @Override
    public void setDividerLocation(final int location) {
        final int size = getOrientation() == VERTICAL_SPLIT ? getHeight() : getWidth();

        final int loc = Math.max(0, Math.min(location, size));
        divider = (double) loc / size;

        super.setDividerLocation(getDividerLocation());
    }
    @Override
    public int getDividerLocation() {
         if (getOrientation() == VERTICAL_SPLIT) {
             return (int)((getHeight() - getDividerSize()) * divider);
         } else {
             return (int)((getWidth() - getDividerSize()) * divider);
         }
    }
}
