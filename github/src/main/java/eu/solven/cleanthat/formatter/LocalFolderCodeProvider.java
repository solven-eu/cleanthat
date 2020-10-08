package eu.solven.cleanthat.formatter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.github.event.GithubPRCodeProvider;
import eu.solven.cleanthat.github.event.ICodeProvider;

/**
 * An {@link ICodeProvider} for local folders
 * 
 * @author Benoit Lacelle
 *
 */
public class LocalFolderCodeProvider implements ICodeProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubPRCodeProvider.class);

	final Path root;

	public LocalFolderCodeProvider(Path root) {
		this.root = root;
	}

	@Override
	public void listFiles(Consumer<Object> consumer) throws IOException {
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

		Files.walk(root).filter(p -> p.toFile().isFile()).filter(gitIgnorePredicate).forEach(consumer);
	}

	@Override
	public boolean fileIsRemoved(Object file) {
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
	public void commitIntoPR(Map<String, String> pathToMutatedContent, List<String> prComments) {
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
	public String loadContent(Object file) throws IOException {
		return Files.readString((Path) file);
	}

	@Override
	public String getFilePath(Object file) {
		Path path = (Path) file;
		return path.subpath(root.getNameCount(), path.getNameCount()).toString();
	}

}
