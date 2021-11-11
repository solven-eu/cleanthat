package eu.solven.cleanthat.codeprovider;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Typically used to be able to read from one {@link ICodeProvider} and write into a different
 * {@link ICodeProviderWriterLogic}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeProviderDecoratingWriter implements ICodeProviderWriter {
	protected final ICodeProvider codeProvider;

	protected final ICodeProviderWriterLogic writerLogic;

	public CodeProviderDecoratingWriter(ICodeProvider codeProvider, ICodeProviderWriterLogic writerLogic) {
		this.codeProvider = codeProvider;
		this.writerLogic = writerLogic;
	}

	@Override
	public void listFiles(Consumer<ICodeProviderFile> consumer) throws IOException {
		codeProvider.listFiles(consumer);
	}

	// @Override
	// public boolean deprecatedFileIsRemoved(Object raw) {
	// return codeProvider.deprecatedFileIsRemoved(raw);
	// }

	@Override
	public String deprecatedLoadContent(Object file) throws IOException {
		return codeProvider.deprecatedLoadContent(file);
	}

	@Override
	public String deprecatedGetFilePath(Object file) {
		return codeProvider.deprecatedGetFilePath(file);
	}

	@Override
	public String getHtmlUrl() {
		return codeProvider.getHtmlUrl();
	}

	@Override
	public String getTitle() {
		return codeProvider.getTitle();
	}

	@Override
	public Optional<String> loadContentForPath(String path) throws IOException {
		return codeProvider.loadContentForPath(path);
	}

	@Override
	public String getRepoUri() {
		return codeProvider.getRepoUri();
	}

	@Override
	public void persistChanges(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cleanTmpFiles() {
		// TODO Auto-generated method stub

	}

}
