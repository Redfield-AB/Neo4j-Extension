/**
 *
 */
package se.redfield.knime.ui;

import java.awt.event.FocusEvent;

import javax.swing.text.DefaultCaret;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class AlwaysVisibleCaret extends DefaultCaret {
    private static final long serialVersionUID = 2842636480072357166L;
    private boolean isFocustLosing = false;

    /**
     * Default constructor.
     */
    public AlwaysVisibleCaret() {
        super();
    }

    @Override
    public void focusLost(final FocusEvent e) {
        isFocustLosing = true;
        try {
            super.focusLost(e);
        } finally {
            isFocustLosing = false;
        }
    }
    @Override
    public void setVisible(final boolean e) {
        // ignore when disabling if focus losing
        if (e || !isFocustLosing) {
            super.setVisible(e);
        }
    }
}
