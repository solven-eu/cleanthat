package eu.solven.cleanthat.codeprovider;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Typically used to be able to read from one {@link ICodeProvider} and write into a different
 * {@link ICodeProviderWriterLogic}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeProviderDecoratingWriter implements ICodeProviderWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeProviderDecoratingWriter.class);
	protected final ICodeProvider codeProvider;

	protected final Supplier<ICodeProviderWriterLogic> writerLogicSupplier;

	public CodeProviderDecoratingWriter(ICodeProvider codeProvider,
			Supplier<ICodeProviderWriterLogic> writerLogicSupplier) {
		this.codeProvider = codeProvider;
		this.writerLogicSupplier = writerLogicSupplier;
	}

	public ICodeProvider getDecorated() {
		return codeProvider;
	}

	@Override
	public void listFiles(Consumer<ICodeProviderFile> consumer) throws IOException {
		codeProvider.listFiles(consumer);
	}

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
		writerLogicSupplier.get().persistChanges(pathToMutatedContent, prComments, prLabels);
	}

	@Override
	public void cleanTmpFiles() {
		LOGGER.debug("Nothing to clean");
	}

}
