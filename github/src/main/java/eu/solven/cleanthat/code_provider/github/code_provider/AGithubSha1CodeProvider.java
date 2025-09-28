/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.code_provider.github.code_provider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import lombok.extern.slf4j.Slf4j;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
@Slf4j
public abstract class AGithubSha1CodeProvider extends AGithubCodeProvider implements IGithubSha1CodeProvider {

	final String token;
	final GHRepository repo;

	final GithubSha1CodeProviderHelper helper;

	public AGithubSha1CodeProvider(Path repositoryRoot, String token, GHRepository repo) {
		super(repositoryRoot);
		this.token = token;

		this.repo = repo;

		this.helper = new GithubSha1CodeProviderHelper(this);
	}

	@Override
	public GHRepository getRepo() {
		return repo;
	}

	@Override
	public String getToken() {
		return token;
	}

	public GithubSha1CodeProviderHelper getHelper() {
		return helper;
	}

	@Override
	public void listFilesForFilenames(Set<String> patterns, Consumer<ICodeProviderFile> consumer) throws IOException {
		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		String sha = getSha1();

		GHTree tree = repo.getTreeRecursive(sha, 1);

		if (tree.isTruncated()) {
			// https://github.community/t/github-get-tree-api-limits-and-recursivity/1300
			LOGGER.info(
					"Tree.isTruncated()=={} -> We will not rely on API to fetch each files, but rather create a local copy (wget zip, git clone, ...)",
					true);
			helper.listFilesLocally(consumer);
		} else {
			processTree(tree, consumer);
		}
	}

	@Override
	public void listFilesForContent(Set<String> patterns, Consumer<ICodeProviderFile> consumer) throws IOException {
		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		String sha = getSha1();

		GHTree tree = repo.getTreeRecursive(sha, 1);

		// https://docs.github.com/en/developers/apps/rate-limits-for-github-apps#server-to-server-requests
		// At best, we will be limited at queries 12,500
		// TODO What is the Tree here? The total number of files in given sha1? Or some diff with parent sha1?
		int treeSize = tree.getTree().size();
		if (treeSize >= helper.getMaxFileBeforeCloning() || helper.hasLocalClone()) {
			LOGGER.info(
					"Tree.size()=={} -> We will not rely on API to fetch each files, but rather create a local copy (wget zip, git clone, ...)",
					treeSize);
			helper.listFilesLocally(consumer);
		} else {
			processTree(tree, consumer);
		}
	}

	private void processTree(GHTree tree, Consumer<ICodeProviderFile> consumer) {
		if (tree.isTruncated()) {
			LOGGER.debug("Should we process some folders independantly?");
		}

		// https://stackoverflow.com/questions/25022016/get-all-file-names-from-a-github-repo-through-the-github-api
		tree.getTree().forEach(ghTreeEntry -> {
			if ("blob".equals(ghTreeEntry.getType())) {
				Path contentPath = CleanthatPathHelpers.makeContentPath(getRepositoryRoot(), ghTreeEntry.getPath());
				consumer.accept(new DummyCodeProviderFile(contentPath, ghTreeEntry));
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
	public Optional<String> loadContentForPath(Path contentPath) throws IOException {
		CleanthatPathHelpers.checkContentPath(contentPath);

		if (helper.localClone.get() != null) {
			// Switch to a raw path as the underlying ICodeProvider has a different root
			var contentRawPath = contentPath.toString();
			// We have a local clone: load the file from it
			return helper.localClone.get().loadContentForPath(contentRawPath);
		} else {
			try {
				String rawPath = CleanthatPathHelpers.makeContentRawPath(getRepositoryRoot(), contentPath);
				return Optional.of(loadContent(repo, rawPath, getSha1()));
			} catch (GHFileNotFoundException e) {
				LOGGER.trace("We miss: {}", contentPath, e);
				LOGGER.debug("We miss: {}", contentPath);
				return Optional.empty();
			}
		}
	}

	@Override
	public String getRepoUri() {
		return repo.getGitTransportUrl();
	}

}
