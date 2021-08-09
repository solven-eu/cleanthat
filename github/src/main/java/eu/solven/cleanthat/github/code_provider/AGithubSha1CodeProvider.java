package eu.solven.cleanthat.github.code_provider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jgit.api.Git;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.code_provider.local.LocalFolderCodeProvider;
import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.jgit.JGitCodeProvider;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public abstract class AGithubSha1CodeProvider extends AGithubCodeProvider implements ICodeProviderWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AGithubSha1CodeProvider.class);

	private static final int MAX_FILE_BEFORE_CLONING = 512;

	private static final boolean ZIP_ELSE_CLONE = true;

	final String token;
	final GHRepository repo;

	final AtomicReference<ICodeProvider> localClone = new AtomicReference<>();

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
		// TODO What is the Tree here? The total number of files in given sha1? Or some diff with parent sha1?
		int treeSize = tree.getTree().size();
		if (treeSize >= MAX_FILE_BEFORE_CLONING || localClone.get() != null) {
			LOGGER.info(
					"Tree.size()=={} -> We will not rely on API to fetch each files, but rather create a local copy (wget zip, git clone, ...)",
					treeSize);
			ensureLocalClone();

			localClone.get().listFiles(consumer);
		} else {
			processTree(tree, consumer);
		}
	}

	@SuppressWarnings("PMD.CloseResource")
	protected boolean ensureLocalClone() {
		// TODO Tests against multiple calls: the repo shall be cloned only once
		synchronized (this) {
			if (localClone.get() != null) {
				// The repo is already cloned
				return false;
			}

			// https://github.community/t/cloning-private-repo-with-a-github-app-private-key/14726
			Path workingDir;
			try {
				workingDir = Files.createTempDirectory("cleanthat-clone");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			ICodeProvider localCodeProvider;
			if (ZIP_ELSE_CLONE) {
				ICodeProvider zippedLocalRef = downloadGitRefLocally(workingDir);
				localCodeProvider = new CodeProviderDecoratingWriter(zippedLocalRef, this);
			} else {
				Git jgit = cloneGitRepoLocally(workingDir);
				localCodeProvider = new JGitCodeProvider(workingDir, jgit, getSha1());
			}
			return localClone.compareAndSet(null, localCodeProvider);
		}
	}

	private void processTree(GHTree tree, Consumer<ICodeProviderFile> consumer) {
		if (tree.isTruncated()) {
			LOGGER.debug("Should we process some folders independantly?");
		}

		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		tree.getTree().forEach(ghTreeEntry -> {
			if ("blob".equals(ghTreeEntry.getType())) {
				consumer.accept(new DummyCodeProviderFile("/" + ghTreeEntry.getPath(), ghTreeEntry));
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

	// @Override
	// public boolean deprecatedFileIsRemoved(Object file) {
	// throw new UnsupportedOperationException("TODO: " + PepperLogHelper.getObjectAndClass(file));
	// }

	@Override
	public void commitIntoBranch(Map<String, String> pathToMutatedContent,
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

	protected Git cloneGitRepoLocally(Path tmpDir) {
		LOGGER.info("Cloning the repo {} into {}", repo.getFullName(), tmpDir);

		String rawTransportUrl = repo.getHttpTransportUrl();
		String authTransportUrl =
				"https://x-access-token:" + token + "@" + rawTransportUrl.substring("https://".length());

		// It seems we are not allowed to give a sha1 as name
		return JGitCodeProvider.makeGitRepo(tmpDir, authTransportUrl, getRef());
	}

	protected ICodeProvider downloadGitRefLocally(Path tmpDir) {
		String ref = getSha1();
		Path zipPath = tmpDir.resolve("repo.zip");
		LOGGER.info("Downloading the repo={} ref={} into {}", repo.getFullName(), ref, zipPath);

		try {
			// https://stackoverflow.com/questions/8377081/github-api-download-zip-or-tarball-link
			// https://docs.github.com/en/rest/reference/repos#download-a-repository-archive-zip
			repo.readZip(inputStream -> {
				long nbBytes = Files.copy(inputStream, zipPath, StandardCopyOption.REPLACE_EXISTING);
				LOGGER.info("We written a ZIP of size={}", PepperLogHelper.humanBytes(nbBytes));
				return tmpDir;
			}, ref);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue downloading a ZIP for " + ref, e);
		}

		Path repoPath = tmpDir.resolve("repo_unzipped");

		// TODO We may want not to unzip the file, but it would probably lead to terrible performance
		LOGGER.info("Unzipping the repo={} ref={} into {}", repo.getFullName(), ref, repoPath);
		try (InputStream fis = Files.newInputStream(zipPath)) {
			unzip(fis, repoPath);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Issue with " + tmpDir, e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return new LocalFolderCodeProvider(tmpDir);
	}

	// https://stackoverflow.com/questions/10633595/java-zip-how-to-unzip-folder
	@SuppressWarnings("PMD.AssignmentInOperand")
	private static void unzip(InputStream is, Path targetDir) throws IOException {
		targetDir = targetDir.toAbsolutePath();
		try (ZipInputStream zipIn = new ZipInputStream(is)) {
			for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null;) {
				Path resolvedPath = targetDir.resolve(ze.getName()).normalize();
				if (!resolvedPath.startsWith(targetDir)) {
					// see: https://snyk.io/research/zip-slip-vulnerability
					throw new RuntimeException("Entry with an illegal path: " + ze.getName());
				}
				if (ze.isDirectory()) {
					Files.createDirectories(resolvedPath);
				} else {
					Files.createDirectories(resolvedPath.getParent());
					Files.copy(zipIn, resolvedPath);
				}
			}
		}
	}
}
