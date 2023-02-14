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
package eu.solven.cleanthat.codeprovider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;

/**
 * Abstract the various ways to iterate over code (Github PR, Gitlab MR, local folder, ...)
 *
 * @author Benoit Lacelle
 */
public interface ICodeProvider {
	Path getRepositoryRoot();

	/**
	 * this will call the consumer on each {@link ICodeProviderFile}. The path will be absolute, considering '/' as the
	 * repository root, with the same FileSystem as {@link ICodeProvider} root.
	 * 
	 * @param consumer
	 * @throws IOException
	 */
	default void listFilesForContent(Consumer<ICodeProviderFile> consumer) throws IOException {
		listFilesForContent(Set.of("glob:**/*"), consumer);
	}

	default void listFilesForFilenames(Consumer<ICodeProviderFile> consumer) throws IOException {
		listFilesForFilenames(Set.of("glob:**/*"), consumer);
	}

	/**
	 * 
	 * @param includes
	 *            a {@link Set} of pattern like 'glob:**\/src/\**\/*.java' or 'regex:.*\/src/.*\/[^/]*\.java'
	 * @param consumer
	 * @throws IOException
	 */
	void listFilesForContent(Set<String> includes, Consumer<ICodeProviderFile> consumer) throws IOException;

	default void listFilesForFilenames(Set<String> includes, Consumer<ICodeProviderFile> consumer) throws IOException {
		listFilesForContent(includes, consumer);
	}

	Optional<String> loadContentForPath(Path path) throws IOException;

	/**
	 * 
	 * @param rawPath
	 * @return
	 * @throws IOException
	 */
	default Optional<String> loadContentForPath(String rawPath) throws IOException {
		Path path = CleanthatPathHelpers.makeContentPath(getRepositoryRoot(), rawPath);

		return loadContentForPath(path);
	}

	String getRepoUri();

}
