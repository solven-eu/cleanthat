package eu.solven.cleanthat.codeprovider;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Abstract the various ways to iterate over code (Github PR, Gitlab MR, local folder, ...)
 *
 * @author Benoit Lacelle
 */
public interface ICodeProvider {

	void listFilesForContent(Consumer<ICodeProviderFile> consumer) throws IOException;

	default void listFilesForFilenames(Consumer<ICodeProviderFile> consumer) throws IOException {
		listFilesForContent(consumer);
	}

	// @Deprecated
	// String deprecatedLoadContent(Object file) throws IOException;

	// @Deprecated
	// String deprecatedGetFilePath(Object file);

	String getHtmlUrl();

	String getTitle();

	Optional<String> loadContentForPath(String path) throws IOException;

	String getRepoUri();

}
