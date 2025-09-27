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
package eu.solven.cleanthat.code_provider.inmemory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.jimfs.Jimfs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.ICodeWritingMetadata;
import lombok.extern.slf4j.Slf4j;

/**
 * An {@link ICodeProvider} for {@link FileSystem}
 *
 * @author Benoit Lacelle
 */
@Slf4j
public class FileSystemCodeProvider implements ICodeProviderWriter {

	final FileSystem fs;
	final Path root;
	final Charset charset;

	@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
			justification = "We need to derive this class in FileSystemGitCodeProvider")
	public FileSystemCodeProvider(Path root, Charset charset) {
		this.fs = root.getFileSystem();
		this.root = root.normalize();
		this.charset = charset;

		if (!this.root.equals(root)) {
			throw new IllegalArgumentException("The root is illegal: " + root);
		}
	}

	public FileSystemCodeProvider(Path root) {
		this(root, StandardCharsets.UTF_8);
	}

	@SuppressWarnings("PMD.CloseResource")
	public static FileSystemCodeProvider forTests() throws IOException {
		var fs = Jimfs.newFileSystem();
		return new FileSystemCodeProvider(CodeProviderHelpers.getRoot(fs), StandardCharsets.UTF_8);
	}

	@Override
	public Path getRepositoryRoot() {
		return root;
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
				var relativized2 = root.relativize(file);

				var rawRelativized = CleanthatPathHelpers.makeContentRawPath(root, relativized2);
				var relativized = CleanthatPathHelpers.makeContentPath(root, rawRelativized);

				consumer.accept(new DummyCodeProviderFile(relativized, file));

				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public String toString() {
		return root.toAbsolutePath().toString();
	}

	private Path resolvePath(Path inMemoryPath) {
		return CleanthatPathHelpers.resolveChild(root, inMemoryPath);
	}

	@Override
	public boolean persistChanges(Map<Path, String> pathToMutatedContent, ICodeWritingMetadata codeWritingMetadata) {
		var hasWritten = new AtomicBoolean();
		pathToMutatedContent.forEach((inMemoryPath, content) -> {
			var resolved = resolvePath(inMemoryPath);
			try {
				Files.createDirectories(resolved.getParent());

				if (Files.exists(resolved)) {
					var existingContent = Files.readString(resolved, charset);

					if (existingContent.equals(content)) {
						LOGGER.info("We skip writing content as same content already present: {}", resolved);
						return;
					}
				}

				LOGGER.info("Write file: {}", resolved);
				Files.write(resolved, content.getBytes(charset));
				hasWritten.set(true);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue on: " + inMemoryPath + " (resolved into " + resolved + ")", e);
			}
		});
		return hasWritten.get();
	}

	@Override
	public Optional<String> loadContentForPath(Path path) throws IOException {
		CleanthatPathHelpers.checkContentPath(path);

		var pathForRootFS = CleanthatPathHelpers.resolveChild(getRepositoryRoot(), path);

		return safeReadString(pathForRootFS);
	}

	private Optional<String> safeReadString(Path pathForRootFS) throws IOException {
		if (Files.exists(pathForRootFS)) {
			String asString;
			try {
				asString = Files.readString(pathForRootFS, charset);
			} catch (MalformedInputException e) {
				LOGGER.warn("Issue reading {}", pathForRootFS, e);
				return Optional.empty();
			}
			return Optional.of(asString);
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
