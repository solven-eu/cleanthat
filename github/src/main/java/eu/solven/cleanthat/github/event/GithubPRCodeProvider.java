package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubPRCodeProvider extends AGithubCodeProvider {

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
		String refName = pr.getHead().getRef();
		GHRepository repo = pr.getRepository();
		GHRef ref;
		try {
			ref = repo.getRef("heads/" + refName);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue fetching the head-ref for: " + pr, e);
		}

		LOGGER.debug("Processing head={} for pr={}", ref, pr);
		commitIntoRef(pathToMutatedContent, prComments, repo, ref);
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
