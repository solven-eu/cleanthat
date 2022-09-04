package eu.solven.cleanthat.code_provider.github.refs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.codeprovider.ICodeProviderWriterLogic;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;

/**
 * Default {@link ICodeProviderWriterLogic}
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubRefWriterLogic implements ICodeProviderWriterLogic {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubRefWriterLogic.class);

	final GHRepository repo;
	final GHRef head;

	public GithubRefWriterLogic(GHRepository repo, GHRef head) {
		this.repo = repo;
		this.head = head;
	}

	@Override
	public void persistChanges(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels) {
		commitIntoRef(pathToMutatedContent, prComments, head);
	}

	protected void commitIntoRef(Map<String, String> pathToMutatedContent, List<String> prComments, GHRef ref) {
		String repoName = repo.getFullName();
		String refName = ref.getRef();
		LOGGER.debug("Persisting into {}:{}", repoName, refName);

		GHTreeBuilder createTree = repo.createTree();
		pathToMutatedContent.forEach((path, content) -> {
			if (!path.startsWith("/")) {
				throw new IllegalStateException("We expect to receive only rooted path: " + path);
			}

			// Remove the leading '/'
			path = path.substring("/".length());

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
			LOGGER.info("Update {}:{} to {} ({})", repoName, refName, newHead, commit.getHtmlUrl());

			ref.updateTo(newHead);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static GHCommitBuilder prepareCommit(GHRepository repo) {
		return repo.createCommit().author("CleanThat", "CleanThat", new Date());
	}
}
