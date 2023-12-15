package se.redfield.knime.neo4j.ui.editor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Class providing the Cypher keywords.
 * 
 * @author Alexander Bondaletov, Redfield SE
 *
 */
public class CypherKeywords {
	private static final NodeLogger LOGGER = NodeLogger.getLogger(CypherKeywords.class);
	private static final String RESOURCE_FILE = "resources/keywords.txt";

	private static List<String> keywords;

	private CypherKeywords() {
	}

	/**
	 * @return The list of keywords.
	 */
	public static List<String> list() {
		if (keywords == null) {
			loadKeywords();
		}
		return Collections.unmodifiableList(keywords);
	}

	private static void loadKeywords() {
		try (Stream<String> stream = Files.lines(getResourcePath())) {
			keywords = stream.collect(Collectors.toList());
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			keywords = List.of();
		}
	}

	private static Path getResourcePath() throws IOException {
		final Bundle thisBundle = FrameworkUtil.getBundle(CypherKeywords.class);
		final URL url = FileLocator.find(thisBundle, new org.eclipse.core.runtime.Path(RESOURCE_FILE), null);
		if (url == null) {
			throw new FileNotFoundException(thisBundle.getLocation() + RESOURCE_FILE);
		}
		return new File(FileLocator.toFileURL(url).getPath()).toPath();
	}
}
