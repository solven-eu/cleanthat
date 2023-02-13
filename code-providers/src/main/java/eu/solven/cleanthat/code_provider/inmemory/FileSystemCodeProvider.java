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
package eu.solven.cleanthat.code_provider.inmemory;

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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;

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

	@SuppressWarnings("PMD.CloseResource")
	public static FileSystemCodeProvider forTests() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newEmpty().build();
		return new FileSystemCodeProvider(CodeProviderHelpers.getRoot(fs));
	}

	@Override
	public FileSystem getFileSystem() {
		return fs;
	}

	@Override
	public void listFilesForContent(Set<String> includes, Consumer<ICodeProviderFile> consumer) throws IOException {
		listFilesForContent(p -> false, consumer);
	}

	protected void listFilesForContent(Predicate<Path> ignorePredicate, Consumer<ICodeProviderFile> consumer)
			throws IOException {
		// https://stackoverflow.com/questions/22867286/files-walk-calculate-total-size/22868706#22868706
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (ignorePredicate.test(dir)) {
					// We skip folders which are ignored, not to process each of their files
					return FileVisitResult.SKIP_SUBTREE;
				} else {
					return FileVisitResult.CONTINUE;
				}
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (ignorePredicate.test(file)) {
					return FileVisitResult.CONTINUE;
				}

				if (!file.startsWith(root)) {
					throw new IllegalStateException("Issue given root=" + root + " and path=" + file);
				}

				// https://stackoverflow.com/questions/58411668/how-to-replace-backslash-with-the-forwardslash-in-java-nio-file-path
				Path relativized = root.relativize(file);

				String unixLikePath = toUnixPath(relativized);

				consumer.accept(new DummyCodeProviderFile("/" + unixLikePath, file));

				return FileVisitResult.CONTINUE;
			}

			private String toUnixPath(Path relativized) {
				String unixLikePath;
				if ("\\".equals(relativized.getFileSystem().getSeparator())) {
					// We get '\' under Windows
					unixLikePath = "/" + relativized.toString().replaceAll(Pattern.quote("\\"), "/");
				} else {
					unixLikePath = relativized.toString();
				}
				return unixLikePath;
			}
		});
	}

	@Override
	public String toString() {
		return root.toAbsolutePath().toString();
	}

	protected Path resolvePath(Path inMemoryPath) {
		if (!inMemoryPath.isAbsolute()) {
			throw new IllegalArgumentException(
					"We expect only absolute path, consider the root of the git repository as the root (path="
							+ inMemoryPath
							+ ")");
		}

		return root.resolve("." + inMemoryPath);
	}

	@Override
	public void persistChanges(Map<Path, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels) {
		pathToMutatedContent.forEach((inMemoryPath, content) -> {
			Path resolved = resolvePath(inMemoryPath);
			try {
				Files.createDirectories(inMemoryPath.getParent());

				LOGGER.info("Write file: {}", resolved);
				Files.write(resolved, content.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new UncheckedIOException("Issue on: " + inMemoryPath + " (resolved into " + resolved + ")", e);
			}
		});
	}

	@Override
	public Optional<String> loadContentForPath(String path) throws IOException {
		Path resolved = resolvePath(getFileSystem().getPath(path));
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
