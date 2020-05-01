/**
 *
 */
package se.redfield.knime.junit;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;

import se.redfield.knime.neo4j.reader.ReaderDialog;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class Neo4JReaderDialogLauncher {
    public static void main(final String[] args) {
        DisableCertsClassLoader.launchMe(Neo4JReaderDialogLauncher.class, "launch");
    }

    public static void launch() throws Throwable {
        final ReaderDialog dialog = new ReaderDialog();

        final JDialog d = new JDialog();
        d.setTitle("Noo4J Reader");
        d.setContentPane(new JPanel(new BorderLayout()));
        ((JPanel) d.getContentPane()).add(dialog.getPanel(), BorderLayout.CENTER);

        d.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
        d.setVisible(true);
    }
}
