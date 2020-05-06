/**
 *
 */
package se.redfield.knime.neo4j.reader;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;

import javax.swing.SwingUtilities;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class UiUtils {
    public static void launchOnParentWindowOpened(final Component c, final Resubmitable r) {
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                final Window w = findOwnerWindow(c);
                if (w == null || !w.isVisible()) {
                    SwingUtilities.invokeLater(this);
                } else {
                    final Runnable withResubmitable = new Runnable() {
                        @Override
                        public void run() {
                            if(r.run()) {
                                SwingUtilities.invokeLater(this);
                            }
                        }
                    };
                    SwingUtilities.invokeLater(withResubmitable);
                }
            }
        };

        SwingUtilities.invokeLater(run);
    }
    private static Window findOwnerWindow(final Component c) {
        Container con = c.getParent();
        while (con != null) {
            if (con instanceof Window) {
                return (Window) con;
            }
            con = con.getParent();
        }

        return null;
    }
}
