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
package eu.solven.cleanthat.code_provider.local;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.code_provider.inmemory.FileSystemCodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.git.GitIgnoreParser;

/**
 * An {@link ICodeProvider} for {@link FileSystem} managing specific Git configuration (especially '.git_ignore' file)
 *
 * @author Benoit Lacelle
 */
public class FileSystemGitCodeProvider extends FileSystemCodeProvider {
	public FileSystemGitCodeProvider(Path root) {
		super(root);
	}

	@Override
	public void listFilesForContent(Set<String> includePatterns, Consumer<ICodeProviderFile> consumer)
			throws IOException {
		var gitIgnorePredicate = makeGitIgnorePredicate();
		listFilesForContent(gitIgnorePredicate, consumer);
	}

	protected Predicate<Path> makeGitIgnorePredicate() throws IOException {
		var gitIgnore = CleanthatPathHelpers.makeContentPath(getRepositoryRoot(), ".gitignore");

		// TODO Beware there could be .gitignore in subfolders
		// TODO Spotless implements this logic
		Predicate<Path> gitIgnorePredicate;
		if (Files.exists(gitIgnore)) {
			var gitIgnoreContent = Files.readString(gitIgnore, StandardCharsets.UTF_8);

			Set<String> patterns = GitIgnoreParser.parsePatterns(gitIgnoreContent);

			gitIgnorePredicate = p -> GitIgnoreParser.match(patterns, p);
		} else {
			gitIgnorePredicate = p -> false;
		}
		return gitIgnorePredicate;
	}
}
