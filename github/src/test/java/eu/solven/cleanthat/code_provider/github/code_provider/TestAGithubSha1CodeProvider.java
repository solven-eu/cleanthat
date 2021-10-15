package eu.solven.cleanthat.code_provider.github.code_provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.function.InputStreamFunction;
import org.mockito.Mockito;
import org.springframework.core.io.FileSystemResource;

import eu.solven.cleanthat.codeprovider.ICodeProvider;

public class TestAGithubSha1CodeProvider {
	final String someRef = "someRef";
	final String someSha1 = "someSha1";

	@Test
	public void testRepoSha1AsZip() throws IOException {
		GHRepository ghRepo = Mockito.mock(GHRepository.class);
		AGithubSha1CodeProvider codeProvider = new AGithubSha1CodeProvider("someToken", ghRepo) {

			@Override
			public String getTitle() {
				return "someTitle";
			}

			@Override
			public String getHtmlUrl() {
				return "someHtmlUrl";
			}

			@Override
			public String getSha1() {
				return someSha1;
			}

			@Override
			public String getRef() {
				return someRef;
			}
		};

		Path tmpZipFile = Files.createTempFile("cleanthat", "TestAGithubSha1CodeProvider.zip");
		tmpZipFile.toFile().delete();

		// https://github.com/google/jimfs
		// https://stackoverflow.com/questions/44459152/provider-not-found-exception-when-creating-a-filesystem-for-my-zip
		{
			Map<String, String> env = new HashMap<>();

			// https://docs.oracle.com/javase/8/docs/technotes/guides/io/fsp/zipfilesystemproviderprops.html
			// Create the zip file if it doesn't exist
			env.put("create", "true");

			// The 'jar:' prefix is necessary to enable the use of FileSystems.newFileSystem over a .zip
			URI uri = URI.create("jar:" + tmpZipFile.toUri());

			try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
				// Path externalTxtFile = Paths.get("/codeSamples/zipfs/SomeTextFile.txt");
				{
					Path pathInZipfile = zipfs.getPath("/root.txt");

					Files.copy(new ByteArrayInputStream("someTiti".getBytes(StandardCharsets.UTF_8)), pathInZipfile);
				}

				{
					Path pathInZipfile = zipfs.getPath("/dir/toto.txt");

					Files.createDirectories(pathInZipfile.getParent());

					Files.copy(new ByteArrayInputStream("someToto".getBytes(StandardCharsets.UTF_8)), pathInZipfile);
				}
			}
		}

		Mockito.when(ghRepo.readZip(Mockito.any(InputStreamFunction.class), Mockito.eq(someSha1))).then(invok -> {
			InputStreamFunction<Object> isf = invok.getArgument(0);

			return isf.apply(new FileSystemResource(tmpZipFile).getInputStream());
		});

		Path tmpDir = Files.createTempDirectory("cleanthat-TestAGithubSha1CodeProvider");
		ICodeProvider localCodeProvider = codeProvider.getHelper().downloadGitRefLocally(tmpDir);

		Set<String> paths = new HashSet<>();
		localCodeProvider.listFiles(file -> {
			paths.add(file.getPath());
		});

		Assertions.assertThat(paths).contains("/root.txt", "/dir/toto.txt");
	}
}
