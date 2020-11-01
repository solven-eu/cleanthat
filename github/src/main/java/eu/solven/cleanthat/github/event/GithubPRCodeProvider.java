package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import eu.solven.cleanthat.formatter.CodeProviderFormatter;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubPRCodeProvider implements ICodeProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubPRCodeProvider.class);

	final GHPullRequest pr;

	public GithubPRCodeProvider(GHPullRequest pr) {
		this.pr = pr;
	}

	@Override
	public void listFiles(Consumer<Object> consumer) throws IOException {
		pr.listFiles().forEach(consumer);
	}

	@Override
	public boolean fileIsRemoved(Object file) {
		return ((GHPullRequestFileDetail) file).getStatus().equals("removed");
	}

	public static String loadContent(GHPullRequest pr, String filename) throws IOException {
		GHRepository repository = pr.getRepository();
		String sha1 = pr.getHead().getSha();
		return loadContent(repository, filename, sha1);
	}

	public static String loadContent(GHRepository repository, String filename, String sha1) throws IOException {
		GHContent content = repository.getFileContent(filename, sha1);
		String asString;
		try (InputStreamReader reader = new InputStreamReader(content.read(), Charsets.UTF_8)) {
			asString = CharStreams.toString(reader);
		}
		return asString;
	}

	public static GHCommitBuilder prepareCommit(GHRepository repo) {
		return repo.createCommit().author("CleanThat", "CleanThat", new Date());
	}

	@Override
	public String getHtmlUrl() {
		return pr.getHtmlUrl().toExternalForm();
	}

	@Override
	public String getTitle() {
		return pr.getTitle();
	}

	@Override
	public void commitIntoPR(Map<String, String> pathToMutatedContent, List<String> prComments) {
		String ref = pr.getHead().getSha();
		GHTreeBuilder createTree = pr.getRepository().createTree();
		pathToMutatedContent.forEach((path, content) -> {
			// TODO isExecutable isn't a parameter from original file?
			createTree.add(path, content, false);
		});
		try {
			GHTree createdTree = createTree.baseTree(ref).create();
			String commitMessage = prComments.stream().collect(Collectors.joining(CodeProviderFormatter.EOL));
			GHCommit commit = prepareCommit(pr.getRepository()).message(commitMessage)
					.parent(ref)
					.tree(createdTree.getSha())
					.create();
			String branchRef = pr.getHead().getRef();
			String newHead = commit.getSHA1();
			LOGGER.info("Update ref {} to {}", branchRef, newHead);
			pr.getRepository().getRef("heads/" + branchRef).updateTo(newHead);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public String loadContent(Object file) throws IOException {
		return loadContent(pr, ((GHPullRequestFileDetail) file).getFilename());
	}

	@Override
	public String getFilePath(Object file) {
		return ((GHPullRequestFileDetail) file).getFilename();
	}
}
