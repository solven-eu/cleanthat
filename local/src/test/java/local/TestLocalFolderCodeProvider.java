package local;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import eu.solven.cleanthat.jgit.LocalFolderCodeProvider;

public class TestLocalFolderCodeProvider {
	final File tmpFolder = org.assertj.core.util.Files.newTemporaryFolder();

	@Test
	public void testLoadAbsolutePathAsRelative() throws IOException {
		Path tmpFolderAsPath = tmpFolder.toPath();
		LocalFolderCodeProvider codeProvider = new LocalFolderCodeProvider(tmpFolderAsPath);

		// Consider a file at the root of given folder
		Files.writeString(tmpFolderAsPath.resolve("cleanthat.yml"), "something");

		Optional<String> optContent = codeProvider.loadContentForPath("/cleanthat.yml");

		Assertions.assertThat(optContent).isPresent().contains("something");
	}

	@Test
	public void testLoadRelativePath() throws IOException {
		Path tmpFolderAsPath = tmpFolder.toPath();
		LocalFolderCodeProvider codeProvider = new LocalFolderCodeProvider(tmpFolderAsPath);

		// Consider a file at the root of given folder
		Files.writeString(tmpFolderAsPath.resolve("cleanthat.yml"), "something");

		Assertions.assertThatThrownBy(() -> codeProvider.loadContentForPath("cleanthat.yml"))
				.isInstanceOf(IllegalArgumentException.class);

		Assertions.assertThatThrownBy(() -> codeProvider.loadContentForPath("./cleanthat.yml"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testLoadNotExistingFile() throws IOException {
		Path tmpFolderAsPath = tmpFolder.toPath();
		LocalFolderCodeProvider codeProvider = new LocalFolderCodeProvider(tmpFolderAsPath);

		// Consider a file at the root of given folder
		Files.writeString(tmpFolderAsPath.resolve("cleanthat.yml"), "something");

		Optional<String> optContent = codeProvider.loadContentForPath("/doesNotExist.yml");

		Assertions.assertThat(optContent).isEmpty();
	}
}
