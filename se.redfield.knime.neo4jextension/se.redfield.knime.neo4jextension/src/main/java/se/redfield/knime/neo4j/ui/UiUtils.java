/**
 *
 */
package se.redfield.knime.neo4j.ui;

import javax.swing.ImageIcon;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public final class UiUtils {
    private UiUtils() {}

    /**
     * @return refresh icon.
     */
    public static ImageIcon createRefreshIcon() {
        return new ImageIcon(UiUtils.class.getClassLoader().getResource("/icons/refresh.png"));
    }
}
