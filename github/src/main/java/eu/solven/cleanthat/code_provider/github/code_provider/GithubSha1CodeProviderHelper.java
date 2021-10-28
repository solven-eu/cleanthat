package eu.solven.cleanthat.code_provider.github.code_provider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jgit.api.Git;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderDecoratingWriter;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.jgit.JGitCodeProvider;

/**
 * Helper for Github sha1
 *
 * @author Benoit Lacelle
 */
public class GithubSha1CodeProviderHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubSha1CodeProviderHelper.class);

	// To be compared with the limit of 5000 calls per hour per installation
	private static final int MAX_FILE_BEFORE_CLONING = 512;

	private static final boolean ZIP_ELSE_CLONE = true;

	final AtomicReference<ICodeProvider> localClone = new AtomicReference<>();

	final IGithubSha1CodeProvider sha1CodeProvider;

	public GithubSha1CodeProviderHelper(IGithubSha1CodeProvider sha1CodeProvider) {
		this.sha1CodeProvider = sha1CodeProvider;
	}

	public int getMaxFileBeforeCloning() {
		return MAX_FILE_BEFORE_CLONING;
	}

	public boolean hasLocalClone() {
		return localClone.get() != null;
	}

	public void listFiles(Consumer<ICodeProviderFile> consumer) throws IOException {
		ensureLocalClone();

		localClone.get().listFiles(consumer);
	}

	/**
	 * 
	 * @return true if we indeed clone locally. False if already cloned locally
	 */
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
				localCodeProvider = new CodeProviderDecoratingWriter(zippedLocalRef, sha1CodeProvider);
			} else {
				Git jgit = cloneGitRepoLocally(workingDir);
				localCodeProvider = new JGitCodeProvider(workingDir, jgit, sha1CodeProvider.getSha1());
			}
			return localClone.compareAndSet(null, localCodeProvider);
		}
	}

	protected Git cloneGitRepoLocally(Path tmpDir) {
		GHRepository repo = sha1CodeProvider.getRepo();
		LOGGER.info("Cloning the repo {} into {}", repo.getFullName(), tmpDir);

		String rawTransportUrl = repo.getHttpTransportUrl();
		String authTransportUrl = "https://x-access-token:" + sha1CodeProvider.getToken()
				+ "@"
				+ rawTransportUrl.substring("https://".length());

		// It seems we are not allowed to give a sha1 as name
		return JGitCodeProvider.makeGitRepo(tmpDir, authTransportUrl, sha1CodeProvider.getRef());
	}

	protected ICodeProvider downloadGitRefLocally(Path tmpDir) {
		String ref = sha1CodeProvider.getSha1();

		// We save the repository zip in this hardcoded file
		Path zipPath = tmpDir.resolve("repository.zip");

		GHRepository repo = sha1CodeProvider.getRepo();
		LOGGER.info("Downloading the repo={} ref={} into {}", repo.getFullName(), ref, zipPath);

		try {
			// https://stackoverflow.com/questions/8377081/github-api-download-zip-or-tarball-link
			// https://docs.github.com/en/rest/reference/repos#download-a-repository-archive-zip
			repo.readZip(inputStream -> {
				long nbBytes = Files.copy(inputStream, zipPath, StandardCopyOption.REPLACE_EXISTING);
				LOGGER.info("We wrote a ZIP of size={}", PepperLogHelper.humanBytes(nbBytes));
				return tmpDir;
			}, ref);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue downloading a ZIP for " + ref, e);
		}

		Path repoPath = tmpDir.resolve("repository");

		// TODO We may want not to unzip the file, but it would probably lead to terrible performance
		LOGGER.info("Unzipping the repo={} ref={} into {}", repo.getFullName(), ref, repoPath);
		try (InputStream fis = Files.newInputStream(zipPath)) {
			unzip(fis, repoPath);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Issue with " + tmpDir, e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return new FileSystemCodeProvider(repoPath);
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
