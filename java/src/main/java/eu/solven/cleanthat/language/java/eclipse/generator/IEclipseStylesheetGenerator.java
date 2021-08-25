package eu.solven.cleanthat.language.java.eclipse.generator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This helps generating a proper Eclipse Stylesheet, based on the existing codebase: it will generate a stylesheet
 * minimizing impacts over the codebase (supposing the codebase is well formatted)
 * 
 * @author Benoit Lacelle
 *
 */
public interface IEclipseStylesheetGenerator {

	Map<Path, String> loadFilesContent(Path path, Pattern compile) throws IOException;

	Map<String, String> generateSettings(Map<Path, String> pathToContent);

}
