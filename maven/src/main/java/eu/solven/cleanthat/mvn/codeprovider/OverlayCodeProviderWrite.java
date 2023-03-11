/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.mvn.codeprovider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(OverlayCodeProviderWrite.class);

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
		pathToOverlay.forEach((path, content) -> {
			consumer.accept(new DummyCodeProviderFile(path, content));
		});

		underlying.listFilesForContent(includes, file -> {
			var path = file.getPath();
			if (pathToOverlay.containsKey(path)) {
				LOGGER.debug("Skip an overlayed path: {}", path);
			} else {
				consumer.accept(file);
			}
		});
	}

	@Override
	public Optional<String> loadContentForPath(Path path) throws IOException {
		CleanthatPathHelpers.checkContentPath(path);

		var overlayedContent = pathToOverlay.get(path);
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
	public boolean persistChanges(Map<Path, String> pathToMutatedContent, ICodeWritingMetadata codeWritingMetadata) {
		SetView<Path> conflicts = Sets.intersection(pathToOverlay.keySet(), pathToMutatedContent.keySet());
		if (!conflicts.isEmpty()) {
			throw new IllegalArgumentException("Can not write into: " + conflicts);
		}

		return underlying.persistChanges(pathToMutatedContent, codeWritingMetadata);
	}

	@Override
	public void cleanTmpFiles() {
		underlying.cleanTmpFiles();
	}

}
