package eu.solven.cleanthat.code_provider.github.code_provider;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;

/**
 * An {@link ICodeProvider} for Github code. Sub-classes manages PR, ref/branches/...
 *
 * @author Benoit Lacelle
 */
public abstract class AGithubCodeProvider implements ICodeProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(AGithubCodeProvider.class);

	public static GHCommitBuilder prepareCommit(GHRepository repo) {
		return repo.createCommit().author("CleanThat", "CleanThat", new Date());
	}

	public static String loadContent(GHRepository repository, String filename, String sha1) throws IOException {
		GHContent content = repository.getFileContent(filename, sha1);
		String asString;
		try (InputStreamReader reader = new InputStreamReader(content.read(), Charsets.UTF_8)) {
			asString = CharStreams.toString(reader);
		}
		return asString;
	}

	protected void commitIntoRef(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			GHRepository repo,
			String refName) {
		GHRef ref;
		try {
			ref = new GithubRepositoryFacade(repo).getRef(refName);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		commitIntoRef(pathToMutatedContent, prComments, repo, ref);
	}

	protected void commitIntoRef(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			GHRepository repo,
			GHRef ref) {
		GHTreeBuilder createTree = repo.createTree();
		pathToMutatedContent.forEach((path, content) -> {
			// TODO isExecutable isn't a parameter from the original file?
			createTree.add(path, content, false);
		});

		// TODO What if the ref has moved to another sha1 in the meantime?
		String sha = ref.getObject().getSha();

		try {
			GHTree createdTree = createTree.baseTree(sha).create();
			String commitMessage = prComments.stream().collect(Collectors.joining(CodeProviderFormatter.EOL));
			GHCommit commit =
					prepareCommit(repo).message(commitMessage).parent(sha).tree(createdTree.getSha()).create();

			String newHead = commit.getSHA1();
			LOGGER.info("Update {} ({}) to {} ({})", getTitle(), getHtmlUrl(), newHead, commit.getHtmlUrl());

			ref.updateTo(newHead);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
