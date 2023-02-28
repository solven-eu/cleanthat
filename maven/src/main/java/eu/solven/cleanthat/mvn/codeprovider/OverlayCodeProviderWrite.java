package eu.solven.cleanthat.mvn.codeprovider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.ICodeWritingMetadata;

/**
 * This {@link ICodeProviderWriter} enables considering some files with a specific read-only content.
 * 
 * @author Benoit Lacelle
 *
 */
public class OverlayCodeProviderWrite implements ICodeProviderWriter {
	final ICodeProviderWriter underlying;

	final Map<Path, String> pathToOverlay;

	public OverlayCodeProviderWrite(ICodeProviderWriter underlying, Map<Path, String> pathToOverlay) {
		this.underlying = underlying;
		this.pathToOverlay = pathToOverlay;
	}

	@Override
	public Path getRepositoryRoot() {
		return underlying.getRepositoryRoot();
	}

	@Override
	public void listFilesForContent(Set<String> includes, Consumer<ICodeProviderFile> consumer) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Optional<String> loadContentForPath(Path path) throws IOException {
		CleanthatPathHelpers.checkContentPath(path);

		String overlayedContent = pathToOverlay.get(path);
		if (overlayedContent != null) {
			return Optional.of(overlayedContent);
		}

		return underlying.loadContentForPath(path);
	}

	@Override
	public String getRepoUri() {
		return underlying.getRepoUri();
	}

	@Override
	public void persistChanges(Map<Path, String> pathToMutatedContent, ICodeWritingMetadata codeWritingMetadata) {
		SetView<Path> conflicts = Sets.intersection(pathToOverlay.keySet(), pathToMutatedContent.keySet());
		if (!conflicts.isEmpty()) {
			throw new IllegalArgumentException("Can not write into: " + conflicts);
		}

		underlying.persistChanges(pathToMutatedContent, codeWritingMetadata);
	}

	@Override
	public void cleanTmpFiles() {
		underlying.cleanTmpFiles();
	}

}
