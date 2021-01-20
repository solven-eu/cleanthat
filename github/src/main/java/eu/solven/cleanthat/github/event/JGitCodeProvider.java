package eu.solven.cleanthat.github.event;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class JGitCodeProvider extends AGithubCodeProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(JGitCodeProvider.class);

	final Path workingDir;
	final Git jgit;
	final String commit;

	public JGitCodeProvider(Path workingDir, Git jgit, String commit) {
		this.workingDir = workingDir;
		this.jgit = jgit;
		this.commit = commit;
	}

	@Override
	public void listFiles(Consumer<ICodeProviderFile> consumer) throws IOException {
		LOGGER.debug("About to list files");

		// TODO count only files relevant given our includes/excludes constrains
		// https://github.blog/2012-09-21-easier-builds-and-deployments-using-git-over-https-and-oauth/
		CheckoutResult.Status checkoutStatus = jgit.checkout().setName(commit).getResult().getStatus();

		if (checkoutStatus != CheckoutResult.Status.OK) {
			throw new IllegalStateException("Issue while checkouting: " + commit);
		}

		// https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/api/ListFilesOfCommitAndTag.java

		String path = "";

		Repository localRepository = jgit.getRepository();
		RevCommit revCommit = buildRevCommit(localRepository, commit);

		// and using commit's tree find the path
		RevTree localTree = revCommit.getTree();

		// shortcut for root-path
		if (path.isEmpty()) {
			try (TreeWalk treeWalk = new TreeWalk(localRepository)) {
				treeWalk.addTree(localTree);
				treeWalk.setRecursive(false);
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
					dirWalk.setRecursive(false);
					while (dirWalk.next()) {
						acceptLocalTreeWalk(consumer, treeWalk);
					}
				}
			}
		}
	}

	private void acceptLocalTreeWalk(Consumer<ICodeProviderFile> consumer, TreeWalk treeWalk) {
		String path = treeWalk.getPathString();
		consumer.accept(new DummyCodeProviderFile(path));
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

	@Override
	public boolean deprecatedFileIsRemoved(Object file) {
		throw new UnsupportedOperationException("TODO");
	}

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
	public void commitIntoPR(Map<String, String> pathToMutatedContent, List<String> prComments) {
		throw new UnsupportedOperationException("TODO");
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

}
