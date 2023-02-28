package eu.solven.cleanthat.code_provider.inmemory;

import java.util.stream.Stream;

public class TestFileSystemCodeProvider {
	@Test
	public void testInMemoryFileSystem() throws IOException {

		Stream.of("").listFilesForContent(file -> {
			System.out.println(s);
		});
	}
}
