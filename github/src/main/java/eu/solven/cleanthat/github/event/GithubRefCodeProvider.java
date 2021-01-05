package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.kohsuke.github.GHBlob;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.kohsuke.github.GHTreeEntry;
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
public class GithubRefCodeProvider extends AGithubCodeProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubRefCodeProvider.class);

	final GHRepository repo;
	final GHRef ref;

	public GithubRefCodeProvider(GHRepository repo, GHRef ref) {
		this.repo = repo;
		this.ref = ref;
	}

	@Override
	public void listFiles(Consumer<Object> consumer) throws IOException {
		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		String sha = ref.getObject().getSha();
		// GHCommit commit = repo.getCommit(sha);

		GHTree tree = repo.getTreeRecursive(sha, 1);

		processTree(tree, consumer);
	}

	private void processTree(GHTree tree, Consumer<Object> consumer) {
		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		tree.getTree().forEach(ghTreeEntry -> {
			if ("blob".equals(ghTreeEntry.getType())) {
				// GHBlob blob;
				// try {
				// blob = ghTreeEntry.asBlob();
				// } catch (IOException e) {
				// throw new UncheckedIOException(e);
				// }
				consumer.accept(ghTreeEntry);
			} else if ("tree".equals(ghTreeEntry.getType())) {
				GHTree subTree;
				try {
					subTree = ghTreeEntry.asTree();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				processTree(subTree, consumer);
			} else {
				LOGGER.debug("Discard: {}", ghTreeEntry);
			}
		});
		// pr.listFiles().forEach(consumer);
	}

	@Override
	public boolean fileIsRemoved(Object file) {
		// TODO Should we check the blob actually exists for given sha1?
		return false;
	}

	public static String loadContent(GHRepository repository, GHRef ref, String filename) throws IOException {
		String sha1 = ref.getObject().getSha();
		return loadContent(repository, filename, sha1);
	}

	@Override
	public String getHtmlUrl() {
		return ref.getUrl().toExternalForm();
	}

	@Override
	public String getTitle() {
		return ref.getRef();
	}

	@Override
	public void commitIntoPR(Map<String, String> pathToMutatedContent, List<String> prComments) {
		commitIntoRef(pathToMutatedContent, prComments, repo, ref);
	}

	@Override
	public String loadContent(Object file) throws IOException {
		return ((GHTreeEntry) file).asBlob().getContent();
	}

	@Override
	public String getFilePath(Object file) {
		return ((GHTreeEntry) file).getPath();
	}
}
