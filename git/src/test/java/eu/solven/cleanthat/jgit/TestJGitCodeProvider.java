package eu.solven.cleanthat.jgit;

import java.nio.file.Paths;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Test;
import org.mockito.Mockito;

import eu.solven.cleanthat.codeprovider.ICodeProviderFile;

public class TestJGitCodeProvider {

	@Test
	public void testAcceptPath() {
		JGitCodeProvider codeProvider = new JGitCodeProvider(Paths.get("any"), Mockito.mock(Git.class), "someSha1");

		Consumer<ICodeProviderFile> consumer = file -> {
			Assertions.assertThat(file.getPath()).startsWith("/").isEqualTo("/root/folder/file");
		};
		TreeWalk treeWalk = Mockito.mock(TreeWalk.class);
		Mockito.when(treeWalk.getPathString()).thenReturn("root/folder/file");
		codeProvider.acceptLocalTreeWalk(consumer, treeWalk);
	}

	@Test
	public void testResolvePath() {
		JGitCodeProvider codeProvider =
				new JGitCodeProvider(Paths.get("/git_root/git_folder"), Mockito.mock(Git.class), "someSha1");

		Assertions.assertThat(codeProvider.resolvePath("/root/folder/file").toString().replace('\\', '/'))
				.isEqualTo("/git_root/git_folder/root/folder/file");
	}
}
