package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Abstract the various ways to iterate over code (Github PR, Gitlab MR, local folder, ...)
 *
 * @author Benoit Lacelle
 */
public interface ICodeProvider {

	// List<?> listFiles() throws IOException;
	void listFiles(Consumer<Object> consumer) throws IOException;

	boolean fileIsRemoved(Object file);

	String getHtmlUrl();

	String getTitle();

	void commitIntoPR(Map<String, String> pathToMutatedContent, List<String> prComments);

	String loadContent(Object file) throws IOException;

	Optional<String> loadContentForPath(String path) throws IOException;

	String getFilePath(Object file);

	// void fileIsChanged(String pathCleanthatJson);
}
