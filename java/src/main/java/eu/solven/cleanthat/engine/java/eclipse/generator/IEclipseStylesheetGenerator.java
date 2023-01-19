package eu.solven.cleanthat.engine.java.eclipse.generator;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
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

	/**
	 * This operation can be very long
	 * 
	 * @param limit
	 *            the maximum duration of the operation
	 * @param pathToContent
	 *            a Map from path to the content to match. These content is typically an existing file for which we are
	 *            looking a relevant formatter stylesheet
	 * @return
	 */
	Map<String, String> generateSettings(OffsetDateTime limit, Map<Path, String> pathToContent);

}
