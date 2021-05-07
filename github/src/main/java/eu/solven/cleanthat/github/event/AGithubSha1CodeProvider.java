package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.jgit.api.Git;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.jgit.JGitCodeProvider;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public abstract class AGithubSha1CodeProvider extends AGithubCodeProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(AGithubSha1CodeProvider.class);

	private static final int MAX_FILE_BEFORE_CLONING = 512;

	final String token;
	final GHRepository repo;

	final AtomicReference<JGitCodeProvider> localClone = new AtomicReference<>();

	public AGithubSha1CodeProvider(String token, GHRepository repo) {
		this.token = token;

		this.repo = repo;
	}

	protected abstract String getSha1();

	protected abstract String getRef();

	@Override
	public void listFiles(Consumer<ICodeProviderFile> consumer) throws IOException {
		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		String sha = getSha1();

		GHTree tree = repo.getTreeRecursive(sha, 1);

		// https://docs.github.com/en/developers/apps/rate-limits-for-github-apps#server-to-server-requests
		// At best, we will be limited at queries 12,500
		if (tree.getTree().size() >= MAX_FILE_BEFORE_CLONING || localClone.get() != null) {
			ensureLocalClone();

			localClone.get().listFiles(consumer);
		} else {
			processTree(tree, consumer);
		}
	}

	@SuppressWarnings("PMD.CloseResource")
	private void ensureLocalClone() {
		// https://github.community/t/cloning-private-repo-with-a-github-app-private-key/14726
		Path workingDir;
		try {
			workingDir = Files.createTempDirectory("cleanthat-clone");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		synchronized (this) {
			Git jgit = makeGitRepo(workingDir);
			localClone.compareAndSet(null, new JGitCodeProvider(workingDir, jgit, getSha1()));
		}
	}

	private void processTree(GHTree tree, Consumer<ICodeProviderFile> consumer) {
		if (tree.isTruncated()) {
			LOGGER.debug("Should we process some folders independantly?");
		}

		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		tree.getTree().forEach(ghTreeEntry -> {
			if ("blob".equals(ghTreeEntry.getType())) {
				consumer.accept(new DummyCodeProviderFile(ghTreeEntry));
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
	public boolean deprecatedFileIsRemoved(Object file) {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public void commitIntoRef(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels
	// ,
	// Optional<String> targetBranch
	) {
		// if (targetBranch.isPresent()) {
		// throw new UnsupportedOperationException("TODO");
		// }

		commitIntoRef(pathToMutatedContent, prComments, repo, getRef());
	}

	@Override
	public String deprecatedLoadContent(Object file) throws IOException {
		String encoding = ((GHTreeEntry) file).asBlob().getEncoding();
		if ("base64".equals(encoding)) {
			// https://github.com/hub4j/github-api/issues/878
			return new String(ByteStreams.toByteArray(((GHTreeEntry) file).asBlob().read()), StandardCharsets.UTF_8);
		} else {
			throw new RuntimeException("TODO Managed encoding: " + encoding);
		}
	}

	@Override
	public String deprecatedGetFilePath(Object file) {
		return ((GHTreeEntry) file).getPath();
	}

	@Override
	public Optional<String> loadContentForPath(String path) throws IOException {
		try {
			return Optional.of(loadContent(repo, path, getSha1()));
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

	public Git makeGitRepo(Path tmpDir) {
		String rawTransportUrl = repo.getHttpTransportUrl();
		String authTransportUrl =
				"https://x-access-token:" + token + "@" + rawTransportUrl.substring("https://".length());

		// It seems we are not allowed to give a sha1 as name
		return JGitCodeProvider.makeGitRepo(tmpDir, authTransportUrl, getRef());
	}
}
