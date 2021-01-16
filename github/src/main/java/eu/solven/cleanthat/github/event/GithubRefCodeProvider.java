package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubRefCodeProvider extends AGithubCodeProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubRefCodeProvider.class);

	final String token;
	final GHRepository repo;
	final GHRef ref;

	public GithubRefCodeProvider(String token, GHRepository repo, GHRef ref) {
		this.token = token;

		this.repo = repo;
		this.ref = ref;
	}

	@Override
	public void listFiles(Consumer<Object> consumer) throws IOException {
		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		String sha = ref.getObject().getSha();

		GHTree tree = repo.getTreeRecursive(sha, 1);

		// https://docs.github.com/en/developers/apps/rate-limits-for-github-apps#server-to-server-requests
		// At best, we will be limited at queries 12,500
		if (tree.getTree().size() >= 1250) {
			// TODO count only files relevant given our includes/excludes constrains
			// https://github.blog/2012-09-21-easier-builds-and-deployments-using-git-over-https-and-oauth/
			makeGitRepo().getRepository();
		} else {
			processTree(tree, consumer);
		}

	}

	private void processTree(GHTree tree, Consumer<Object> consumer) {
		if (tree.isTruncated()) {
			LOGGER.debug("Should we process some folders independantly?");
		}

		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		tree.getTree().forEach(ghTreeEntry -> {
			if ("blob".equals(ghTreeEntry.getType())) {
				consumer.accept(ghTreeEntry);
			} else if ("tree".equals(ghTreeEntry.getType())) {
				LOGGER.debug("Discard tree as original call for tree was recursive: {}", ghTreeEntry);

				// GHTree subTree;
				// try {
				// subTree = ghTreeEntry.asTree();
				// } catch (IOException e) {
				// throw new UncheckedIOException(e);
				// }
				// // TODO This can lead with very deep stack: BAD. Switch to a queue
				// processTree(subTree, consumer);
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
		String encoding = ((GHTreeEntry) file).asBlob().getEncoding();
		if ("base64".equals(encoding)) {
			// https://github.com/hub4j/github-api/issues/878
			return new String(ByteStreams.toByteArray(((GHTreeEntry) file).asBlob().read()), StandardCharsets.UTF_8);
		} else {
			throw new RuntimeException("TODO Managed encoding: " + encoding);
		}
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

	@Override
	public String getRepoUri() {
		return repo.getGitTransportUrl();
	}

	@Override
	public Git makeGitRepo() {
		// https://github.community/t/cloning-private-repo-with-a-github-app-private-key/14726
		Path tmpDir;
		try {
			tmpDir = Files.createTempDirectory("cleanthat-clone");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		// v1.5c177cb5229fa3f27e85f7472881be4022d58f20
		String rawTransportUrl = repo.getHttpTransportUrl();
		String authTransportUrl =
				"https://x-access-token:" + token + "@" + rawTransportUrl.substring("https://".length());

		try {
			return Git.cloneRepository()
					.setURI(authTransportUrl)
					.setDirectory(tmpDir.toFile())
					.setBranch(ref.getRef())
					.setCloneAllBranches(false)
					.setCloneSubmodules(false)
					.setProgressMonitor(new TextProgressMonitor())
					.call();
		} catch (GitAPIException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
