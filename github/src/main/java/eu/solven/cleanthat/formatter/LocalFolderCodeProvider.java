package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public List<Path> listFiles() throws IOException {
		return Files.walk(root).filter(p -> p.toFile().isFile()).collect(Collectors.toList());
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
				Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
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
