package se.redfield.knime.neo4j.ui.editor;

import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.border.EtchedBorder;
import javax.swing.text.BadLocationException;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionCellRenderer;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.rsyntaxtextarea.KnimeSyntaxTextArea;

import se.redfield.knime.neo4j.connector.FunctionDesc;
import se.redfield.knime.neo4j.connector.NamedWithProperties;
import se.redfield.knime.neo4j.db.LabelsAndFunctions;

/**
 * The source editor for the Cypher scripts.
 * 
 * @author Alexander Bondaletov, Redfield SE
 *
 */
public class CypherEditor {
	private static final int FONT_SIZE = 14;
	private static final String CYPHER_SYNTAX = "text/cypher";
	static {
		AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
		atmf.putMapping(CYPHER_SYNTAX, "se.redfield.knime.neo4j.ui.editor.CypherTokenMaker",
				CypherEditor.class.getClassLoader());
	}

	private KnimeSyntaxTextArea editor;

	private AutoCompletion currentAutoCompletion;

	private RTextScrollPane scrollpane;

	public CypherEditor() {
		editor = new KnimeSyntaxTextArea();
		editor.setSyntaxEditingStyle(CYPHER_SYNTAX);
		editor.setCodeFoldingEnabled(true);
		editor.setAntiAliasingEnabled(true);
		editor.setAutoIndentEnabled(true);
		editor.setFadeCurrentLineHighlight(true);
		editor.setHighlightCurrentLine(false);
		editor.setLineWrap(true);
		editor.setRoundedSelectionEdges(true);
		editor.setBorder(new EtchedBorder());
		editor.setTabSize(4);
		editor.setFont(new Font(editor.getFont().getName(), editor.getFont().getStyle(), FONT_SIZE));

		scrollpane = new RTextScrollPane(editor);
		scrollpane.setPreferredSize(new Dimension(300, 150));

		installAutoComplete(null);
	}

	/**
	 * Installs the autocomplete updated from the provided meta.
	 * 
	 * @param meta The database meta.
	 */
	public void installAutoComplete(LabelsAndFunctions meta) {
		if (currentAutoCompletion != null) {
			currentAutoCompletion.uninstall();
		}

		currentAutoCompletion = new AutoCompletion(createCompletionProvider(meta));

		CompletionCellRenderer renderer = new CompletionCellRenderer();
		renderer.setDisplayFont(new Font(renderer.getFont().getName(), renderer.getFont().getStyle(), FONT_SIZE));
		currentAutoCompletion.setListCellRenderer(renderer);

		currentAutoCompletion.setShowDescWindow(true);
		currentAutoCompletion.install(editor);
	}

	private CompletionProvider createCompletionProvider(LabelsAndFunctions meta) {
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

		for (String keyword : CypherKeywords.list()) {
			provider.addCompletion(new BasicCompletion(provider, keyword, "Keyword"));
		}

		if (meta != null) {
			setupCompletion(provider, meta.getNodes(), "Node");
			setupCompletion(provider, meta.getRelationships(), "Relationship");

			for (FunctionDesc func : meta.getFunctions()) {
				provider.addCompletion(new BasicCompletion(provider, func.getName(), "Function",
						"<b>" + func.getSignature() + "</b><br/><br/>" + func.getDescription()));
			}
		}

		return provider;
	}

	private void setupCompletion(DefaultCompletionProvider provider, List<NamedWithProperties> items,
			String shortDesc) {
		for (NamedWithProperties item : items) {
			provider.addCompletion(new BasicCompletion(provider, item.getName(), shortDesc));

			for (String property : item.getProperties()) {
				provider.addCompletion(
						new BasicCompletion(provider, property, "Property", "Property of '" + item.getName() + "'"));
			}
		}
	}

	/**
	 * @return The component.
	 */
	public JComponent getComponent() {
		return scrollpane;
	}

	/**
	 * @return The text contained in the editor.
	 */
	public String getText() {
		return editor.getText();
	}

	/**
	 * Sets the text of the editor.
	 *
	 * @param text The text to set
	 */
	public void setText(final String text) {
		editor.setText(text);
	}

	/**
	 * @param enabled Enabled status.
	 */
	public void setEnabled(boolean enabled) {
		editor.setEnabled(enabled);
	}

	/**
	 * Inserts the given text into the editor.
	 * 
	 * @param text The text to insert.
	 */
	public void insert(String text) {
		// possible remove selection
		final int selStart = editor.getSelectionStart();
		final int selEnd = editor.getSelectionEnd();
		if (selEnd != selStart) {
			try {
				editor.getDocument().remove(Math.min(selStart, selEnd), Math.abs(selStart - selEnd));
			} catch (final BadLocationException e) {
				// ignore
			}
		}

		// insert text
		final int pos = Math.max(0, editor.getCaretPosition());
		editor.insert(text, pos);
	}
}
