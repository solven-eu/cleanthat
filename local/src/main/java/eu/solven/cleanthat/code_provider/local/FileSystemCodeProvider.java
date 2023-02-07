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

import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.git.GitIgnoreParser;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ICodeProvider} for {@link FileSystem}
 *
 * @author Benoit Lacelle
 */
public class FileSystemCodeProvider implements ICodeProviderWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemCodeProvider.class);

	final FileSystem fs;
	final Path root;

	public FileSystemCodeProvider(FileSystem fs, Path root) {
		this.fs = fs;
		this.root = root;
	}

	public FileSystemCodeProvider(Path root) {
		this(root.getFileSystem(), root);
	}

	@Override
	public FileSystem getFileSystem() {
		return fs;
	}

	@Override
	public void listFilesForContent(Set<String> includePatterns, Consumer<ICodeProviderFile> consumer)
			throws IOException {
		Predicate<Path> gitIgnorePredicate;

		File gitIgnore = root.resolve(fs.getPath(".gitignore")).toFile();

		// TODO Beware there could be .gitignore in subfolders
		if (gitIgnore.isFile()) {
			String gitIgnoreContent = Files.readString(gitIgnore.toPath(), StandardCharsets.UTF_8);

			Set<String> patterns = GitIgnoreParser.parsePatterns(gitIgnoreContent);

			gitIgnorePredicate = p -> GitIgnoreParser.accept(patterns, p);
		} else {
			gitIgnorePredicate = p -> true;
		}
		// https://stackoverflow.com/questions/22867286/files-walk-calculate-total-size/22868706#22868706
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (gitIgnorePredicate.test(dir)) {
					return FileVisitResult.CONTINUE;
				} else {
					// We skip folders which are ignored, not to process each of their files
					return FileVisitResult.SKIP_SUBTREE;
				}
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!gitIgnorePredicate.test(file)) {
					return FileVisitResult.CONTINUE;
				}

				if (!file.startsWith(root)) {
					throw new IllegalStateException("Issue given root=" + root + " and path=" + file);
				}

				// https://stackoverflow.com/questions/58411668/how-to-replace-backslash-with-the-forwardslash-in-java-nio-file-path
				Path relativized = root.relativize(file);
				// We get '\' under Windows
				String pathWithSlash = "/" + relativized.toString().replaceAll("\\\\", "/");

				consumer.accept(new DummyCodeProviderFile(pathWithSlash, file));

				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public String toString() {
		return root.toAbsolutePath().toString();
	}

	protected Path resolvePath(String path) {
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException(
					"We expect only absolute path, consider the root of the git repository as the root (path=" + path
							+ ")");
		}

		return root.resolve("." + path);
	}

	@Override
	public void persistChanges(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels) {
		pathToMutatedContent.forEach((path, content) -> {
			Path resolved = resolvePath(path);
			try {
				LOGGER.info("Write file: {}", path);
				Files.write(resolved, content.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new UncheckedIOException("Issue on: " + path + " (resolved into " + resolved + ")", e);
			}
		});
	}

	// @Override
	// public String deprecatedLoadContent(Object file) throws IOException {
	// return Files.readString((Path) file);
	// }
	//
	// @Override
	// public String deprecatedGetFilePath(Object rawFile) {
	// Path file = (Path) rawFile;
	// return file.subpath(root.getNameCount(), file.getNameCount()).toString();
	// }

	@Override
	public Optional<String> loadContentForPath(String path) throws IOException {
		Path resolved = resolvePath(path);
		if (resolved.toFile().isFile()) {
			return Optional.of(Files.readString(resolved));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public String getRepoUri() {
		throw new IllegalArgumentException("No repository URI");
	}

	@Override
	public void cleanTmpFiles() {
		LOGGER.info("Nothing to delete for {}", this);
	}
}
