/**
 *
 */
package se.redfield.knime.neo4jextension.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class StringSelectionPane extends JPanel {
    private static final long serialVersionUID = 3466844497286385247L;

    private final DefaultListModel<String> leftSide = new DefaultListModel<String>();
    private final DefaultListModel<String> rightSide = new DefaultListModel<String>();

    private final DefaultButtonModel remove = new DefaultButtonModel();
    private final DefaultButtonModel add = new DefaultButtonModel();

    private JSplitPane splitPane;
    private final Comparator<String> ignoreCaseComparator = new Comparator<String>() {
        @Override
        public int compare(final String o1, final String o2) {
            return o1.toLowerCase().compareTo(o2.toLowerCase());
        }
    };

    public StringSelectionPane() {
        super(new BorderLayout(5, 5));

        add(createButtonsPanel(), BorderLayout.NORTH);
        //left side
        final JList<String> leftList = createList(leftSide, add);

        //right side
        final JList<String> rightList = createList(rightSide, remove);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(leftList,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                new JScrollPane(rightList,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        add(splitPane, BorderLayout.CENTER);

//        add(createButtonsPanel(), BorderLayout.SOUTH);

        //add listeners.
        add.addActionListener(e -> doMoveSelected(leftList, rightSide));
        remove.addActionListener(e -> doMoveSelected(rightList, leftSide));
    }
    private void doMoveSelected(final JList<String> sourceList, final DefaultListModel<String> target) {
        final int[] indexes = sourceList.getSelectedIndices();
        final DefaultListModel<String> source = (DefaultListModel<String>) sourceList.getModel();

        for(int i = indexes.length - 1; i >=0; i--) {
            final int index = indexes[i];

            final String element = source.remove(index);
            final int pos = getPositionFor(element, target);

            if (pos < target.getSize()) {
                target.add(pos, element);
            } else {
                target.addElement(element);
            }
        }
    }
    /**
     * @param element element to insert.
     * @param target target model.
     * @return
     */
    private int getPositionFor(final String element, final DefaultListModel<String> target) {
        final int size = target.getSize();
        for (int i = 0; i < size; i++) {
            final String that = target.get(i);
            if (ignoreCaseComparator.compare(element, that) < 0) {
                return i;
            }
        }
        return size;
    }
    private JList<String> createList(final DefaultListModel<String> model, final DefaultButtonModel buttonModel) {
        //disable button model at initial
        buttonModel.setEnabled(false);

        //create list
        final JList<String> list = new JList<String>(model);
        //add list selection listening
        list.addListSelectionListener(e -> selectionChanged(list, buttonModel));
        return list;
    }

    private void selectionChanged(final JList<String> list, final DefaultButtonModel buttonModel) {
        final int[] selected = list.getSelectedIndices();
        final boolean enabled = selected != null && selected.length > 0;
        buttonModel.setEnabled(enabled);
    }

    /**
     * @return
     */
    private JPanel createButtonsPanel() {
        final JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));

        final JPanel buttons = new JPanel(new GridLayout(1, 2));
        p.add(buttons);

        final JButton buttonAdd = new JButton(">>");
        buttonAdd.setModel(add);
        buttons.add(buttonAdd);

        final JButton buttonRemove = new JButton("<<");
        buttonRemove.setModel(remove);
        buttons.add(buttonRemove);

        return p;
    }

    public void init(final List<String> source, final List<String> target) {
        clear();

        Collections.sort(source, ignoreCaseComparator);
        Collections.sort(target, ignoreCaseComparator);

        for (final String str : source) {
            leftSide.addElement(str);
        }
        for (final String str : target) {
            rightSide.addElement(str);
        }

        splitPane.doLayout();
        splitPane.setDividerLocation(0.5);
    }

    private void clear() {
        leftSide.removeAllElements();
        rightSide.removeAllElements();
    }
    public List<String> getSelection() {
        final List<String> list = new LinkedList<>();
        final int size = rightSide.getSize();
        for (int i = 0; i < size; i++) {
            list.add(rightSide.getElementAt(i));
        }
        return list;
    }

    public static void main(final String[] args) {
        final JDialog test = new JDialog();
        test.setTitle("Test");

        test.setContentPane(new JPanel(new BorderLayout()));

        final StringSelectionPane p = new StringSelectionPane();
        ((JPanel) test.getContentPane()).add(p, BorderLayout.CENTER);

        final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();

        test.setSize(size.width * 2 / 3, size.height * 2 / 3);

        //init with data
        final List<String> soource = new LinkedList<>();
        soource.add("Однажды");
        soource.add("в студеную");
        soource.add("зимнюю");
        soource.add("пору");
        soource.add("я из лесу вышел");

        final List<String> target = new LinkedList<>();
        target.add("был сильный мороз");

        p.init(soource, target);

        //show
        test.setLocationByPlatform(true);
        test.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        test.setVisible(true);
    }
}
