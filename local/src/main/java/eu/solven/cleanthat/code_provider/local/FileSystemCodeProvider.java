package eu.solven.cleanthat.code_provider.local;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.git.GitIgnoreParser;

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
	public void listFilesForContent(Consumer<ICodeProviderFile> consumer) throws IOException {
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
		Files.walk(root).filter(p -> p.toFile().isFile()).filter(gitIgnorePredicate).forEach(f -> {
			if (!f.startsWith(root)) {
				throw new IllegalStateException("Issue given root=" + root + " and path=" + f);
			}

			// https://stackoverflow.com/questions/58411668/how-to-replace-backslash-with-the-forwardslash-in-java-nio-file-path
			Path relativized = root.relativize(f);
			// We get '\' under Windows
			String pathWithSlash = "/" + relativized.toString().replaceAll("\\\\", "/");
			consumer.accept(new DummyCodeProviderFile(pathWithSlash, f));
		});
	}

	@Override
	public String getHtmlUrl() {
		return root.toAbsolutePath().toString();
	}

	@Override
	public String getTitle() {
		return root.getFileName().toString();
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

//	@Override
//	public String deprecatedLoadContent(Object file) throws IOException {
//		return Files.readString((Path) file);
//	}
//
//	@Override
//	public String deprecatedGetFilePath(Object rawFile) {
//		Path file = (Path) rawFile;
//		return file.subpath(root.getNameCount(), file.getNameCount()).toString();
//	}

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
