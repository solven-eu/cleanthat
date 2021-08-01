package eu.solven.cleanthat.code_provider.local;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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

import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;

/**
 * An {@link ICodeProvider} for local folders
 *
 * @author Benoit Lacelle
 */
public class LocalFolderCodeProvider implements ICodeProviderWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalFolderCodeProvider.class);

	final Path root;

	public LocalFolderCodeProvider(Path root) {
		LOGGER.info("root={}", root);
		this.root = root;
	}

	@Override
	public void listFiles(Consumer<ICodeProviderFile> consumer) throws IOException {
		File gitIgnore = root.resolve(".gitignore").toFile();
		Predicate<Path> gitIgnorePredicate;
		if (gitIgnore.isFile()) {
			Set<String> lines = ImmutableSet.copyOf(Files.readAllLines(gitIgnore.toPath(), StandardCharsets.UTF_8));
			gitIgnorePredicate = p -> {
				for (int i = 0; i < p.getNameCount(); i++) {
					// This will typically match the exclusion of 'target' (and 'target/')
					if (lines.contains(p.getName(i).toString()) || lines.contains(p.getName(i).toString() + "/")) {
						return false;
					}
				}
				return true;
			};
		} else {
			gitIgnorePredicate = p -> true;
		}
		Files.walk(root)
				.filter(p -> p.toFile().isFile())
				.filter(gitIgnorePredicate)
				.forEach(f -> consumer.accept(new DummyCodeProviderFile(f)));
	}

	@Override
	public boolean deprecatedFileIsRemoved(Object file) {
		// This class sees only existing files
		return false;
	}

	@Override
	public String getHtmlUrl() {
		return root.toAbsolutePath().toString();
	}

	@Override
	public String getTitle() {
		return root.getFileName().toString();
	}

	@Override
	public void commitIntoBranch(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels) {
		pathToMutatedContent.forEach((path, content) -> {
			try {
				LOGGER.info("Write file: {}", path);
				Files.write(root.resolve(path), content.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new UncheckedIOException("Issue on: " + path, e);
			}
		});
	}

	@Override
	public String deprecatedLoadContent(Object file) throws IOException {
		return Files.readString((Path) file);
	}

	@Override
	public String deprecatedGetFilePath(Object rawFile) {
		Path file = (Path) rawFile;
		return file.subpath(root.getNameCount(), file.getNameCount()).toString();
	}

	@Override
	public Optional<String> loadContentForPath(String path) throws IOException {
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException(
					"We expect only absolute path, consider the root of the git repository as the root (path=" + path
							+ ")");
		}

		Path resolved = root.resolve("." + path);
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

}
