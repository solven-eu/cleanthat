/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.codeprovider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

/**
 * Typically used to be able to read from one {@link ICodeProvider} and write into a different
 * {@link ICodeProviderWriterLogic}
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class CodeProviderDecoratingWriter implements ICodeProviderWriter {
	protected final ICodeProvider codeProvider;

	protected final Supplier<ICodeProviderWriterLogic> writerLogicSupplier;

	public CodeProviderDecoratingWriter(ICodeProvider codeProvider,
			Supplier<ICodeProviderWriterLogic> writerLogicSupplier) {
		this.codeProvider = codeProvider;
		this.writerLogicSupplier = writerLogicSupplier;
	}

	@Override
	public Path getRepositoryRoot() {
		return codeProvider.getRepositoryRoot();
	}

	public ICodeProvider getDecorated() {
		return codeProvider;
	}

	@Override
	public void listFilesForContent(Set<String> includePatterns, Consumer<ICodeProviderFile> consumer)
			throws IOException {
		codeProvider.listFilesForContent(includePatterns, consumer);
	}

	@Override
	public String toString() {
		return codeProvider.toString();
	}

	@Override
	public Optional<String> loadContentForPath(Path path) throws IOException {
		return codeProvider.loadContentForPath(path);
	}

	@Override
	public String getRepoUri() {
		return codeProvider.getRepoUri();
	}

	@Override
	public boolean persistChanges(Map<Path, String> pathToMutatedContent, ICodeWritingMetadata metadata) {
		return writerLogicSupplier.get().persistChanges(pathToMutatedContent, metadata);
	}

	@Override
	public void cleanTmpFiles() {
		LOGGER.debug("Nothing to clean");
	}

}
