package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Override
	public Optional<String> loadContentForPath(String path) throws IOException {
		try {
			return Optional.of(loadContent(repo, path, ref.getObject().getSha()));
		} catch (GHFileNotFoundException e) {
			LOGGER.trace("We miss: {}", path, e);
			LOGGER.debug("We miss: {}", path);
			return Optional.empty();
		}
	}
}
