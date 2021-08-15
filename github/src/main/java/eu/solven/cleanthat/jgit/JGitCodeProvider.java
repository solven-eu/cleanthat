package eu.solven.cleanthat.jgit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubCodeProvider;
import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
@SuppressWarnings("PMD.GodClass")
public class JGitCodeProvider extends AGithubCodeProvider implements ICodeProviderWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(JGitCodeProvider.class);

	final Path workingDir;
	final Git jgit;
	final String commit;

	public JGitCodeProvider(Path workingDir, Git jgit, String commit) {
		this.workingDir = workingDir;
		this.jgit = jgit;
		this.commit = commit;

		Status status;
		try {
			status = jgit.status().call();
		} catch (NoWorkTreeException | GitAPIException e) {
			throw new IllegalStateException("Issue while checking repository status", e);
		}
		if (status.hasUncommittedChanges()) {
			throw new IllegalArgumentException("We expect to work on a clean repository");
		}

		// try {
		// String fullBranchName = jgit.getRepository().getFullBranch();
		String head = getHeadName(jgit.getRepository());
		if (!commit.equals(head)) {
			throw new IllegalArgumentException("Invalid current sh1: " + head + " (expected: " + commit + ")");
		}
		// } catch (IOException e) {
		// throw new UncheckedIOException("Issue getting current branch name", e);
		// }
	}

	@Override
	public void listFiles(Consumer<ICodeProviderFile> consumer) throws IOException {
		LOGGER.debug("About to list files");

		walkFiles(consumer, "");
	}

	// https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/api/ListFilesOfCommitAndTag.java
	private void walkFiles(Consumer<ICodeProviderFile> consumer, String path) throws IOException {
		Repository localRepository = jgit.getRepository();
		RevCommit revCommit = buildRevCommit(localRepository, commit);

		// and using commit's tree find the path
		RevTree localTree = revCommit.getTree();

		// shortcut for root-path
		if (path.isEmpty()) {
			try (TreeWalk treeWalk = new TreeWalk(localRepository)) {
				treeWalk.addTree(localTree);
				treeWalk.setRecursive(true);
				treeWalk.setPostOrderTraversal(false);

				while (treeWalk.next()) {
					acceptLocalTreeWalk(consumer, treeWalk);
				}
			}
		} else {
			// now try to find a specific file
			try (TreeWalk treeWalk = buildTreeWalk(localRepository, localTree, path)) {
				if ((treeWalk.getFileMode(0).getBits() & FileMode.TYPE_TREE) == 0) {
					throw new IllegalStateException("Tried to read the elements of a non-tree for commit '" + commit
							+ "' and path '"
							+ path
							+ "', had filemode "
							+ treeWalk.getFileMode(0).getBits());
				}

				try (TreeWalk dirWalk = new TreeWalk(localRepository)) {
					dirWalk.addTree(treeWalk.getObjectId(0));
					dirWalk.setRecursive(true);
					while (dirWalk.next()) {
						acceptLocalTreeWalk(consumer, treeWalk);
					}
				}
			}
		}
	}

	private void acceptLocalTreeWalk(Consumer<ICodeProviderFile> consumer, TreeWalk treeWalk) {
		String path = treeWalk.getPathString();
		consumer.accept(new DummyCodeProviderFile(path, path));
	}

	private static RevCommit buildRevCommit(Repository repository, String commit) throws IOException {
		// a RevWalk allows to walk over commits based on some filtering that is defined
		try (RevWalk revWalk = new RevWalk(repository)) {
			return revWalk.parseCommit(ObjectId.fromString(commit));
		}
	}

	private static TreeWalk buildTreeWalk(Repository repository, RevTree tree, final String path) throws IOException {
		TreeWalk treeWalk = TreeWalk.forPath(repository, path, tree);

		if (treeWalk == null) {
			throw new FileNotFoundException(
					"Did not find expected file '" + path + "' in tree '" + tree.getName() + "'");
		}

		return treeWalk;
	}

	// @Override
	// public boolean deprecatedFileIsRemoved(Object file) {
	// String relativePath = (String) file;
	// Path path = workingDir.resolve(relativePath);
	//
	// if (!path.startsWith(workingDir)) {
	// // https://stackoverflow.com/questions/53157337/validate-to-prevent-a-path-string-to-go-up-to-parent-folder
	// throw new IllegalArgumentException("Can not move out of the parent folder");
	// }
	//
	// return !path.toFile().exists();
	// }

	public static String loadContent(GHRepository repository, GHRef ref, String filename) throws IOException {
		String sha1 = ref.getObject().getSha();
		return loadContent(repository, filename, sha1);
	}

	@Override
	public String getHtmlUrl() {
		try {
			return jgit.remoteList().call().get(0).getURIs().get(0).getPath();
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getTitle() {
		return "Local JGit";
	}

	@Override
	public void commitIntoBranch(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels
	// , Optional<String> targetBranch
	) {
		// targetBranch

		pathToMutatedContent.forEach((k, v) -> {
			Path resolvedPath = workingDir.resolve((String) k);

			try {
				Files.writeString(resolvedPath, v, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		// https://stackoverflow.com/questions/12734760/jgit-how-to-add-all-files-to-staging-area
		try {
			jgit.add().addFilepattern(".").call();
			LOGGER.info("Added");
		} catch (GitAPIException e) {
			throw new RuntimeException("Issue adding all files into staging area", e);
		}

		try {
			jgit.commit().setMessage(prComments.stream().collect(Collectors.joining("\r\n"))).call();
			LOGGER.info("Committed");
		} catch (GitAPIException e) {
			throw new RuntimeException("Issue committing", e);
		}

		try {
			jgit.push().call();
			LOGGER.info("Pushed");
		} catch (GitAPIException e) {
			throw new RuntimeException("Issue pushing", e);
		}
	}

	@Override
	public String deprecatedLoadContent(Object file) throws IOException {
		Path resolvedPath = workingDir.resolve((String) file);

		return new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8);
	}

	@Override
	public String deprecatedGetFilePath(Object file) {
		return file.toString();
	}

	@Override
	public Optional<String> loadContentForPath(String path) throws IOException {
		Path resolvedPath = workingDir.resolve(path);

		if (resolvedPath.toFile().isFile()) {
			return Optional.of(new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public String getRepoUri() {
		return "TODO";
	}

	public static String getHeadName(Repository repo) {
		try {
			ObjectId id = repo.resolve(Constants.HEAD);
			return id.getName();
		} catch (IOException e) {
			throw new UncheckedIOException("Issue fetching the head sha1", e);
		}
	}

	/**
	 * 
	 * @param dir
	 * @param authTransportUrl
	 * @param branch
	 *            refname or branchName or tagName
	 * @return
	 */
	public static Git makeGitRepo(Path dir, String authTransportUrl, String branch) {
		LOGGER.info("Cloning a repo into {}", dir);

		// https://stackoverflow.com/questions/11475263/shallow-clone-with-jgit
		CloneCommand builder = Git.cloneRepository()
				.setURI(authTransportUrl)
				.setDirectory(dir.toFile())
				.setBranch(branch)
				.setCloneAllBranches(false)
				.setCloneSubmodules(false)
				.setProgressMonitor(new TextProgressMonitor());

		String userInfo = URI.create(authTransportUrl).getUserInfo();
		if (!Strings.isNullOrEmpty(userInfo) && userInfo.indexOf(':') >= 0) {
			int indexofSemiColumn = userInfo.indexOf(':');
			// Unclear why, sometimes, JGit fails with BASIC in provided URL: we help it through an explicit
			// CredentialProvider
			builder.setCredentialsProvider(
					new UsernamePasswordCredentialsProvider(userInfo.substring(0, indexofSemiColumn),
							userInfo.substring(indexofSemiColumn + 1)));
		}

		try {
			return builder.call();
		} catch (GitAPIException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
