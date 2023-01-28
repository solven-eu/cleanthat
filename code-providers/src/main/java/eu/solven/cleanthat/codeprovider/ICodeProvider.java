/*
 * Copyright 2023 Solven
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
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Abstract the various ways to iterate over code (Github PR, Gitlab MR, local folder, ...)
 *
 * @author Benoit Lacelle
 */
public interface ICodeProvider {
	FileSystem getFileSystem();

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

	Optional<String> loadContentForPath(String path) throws IOException;

	default Optional<String> loadContentForPath(Path path) throws IOException {
		return loadContentForPath(path.toString());
	}

	String getRepoUri();

}
