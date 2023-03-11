/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.code_provider.github.refs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.code_provider.github.code_provider.AGithubSha1CodeProvider;
import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.ICodeWritingMetadata;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import eu.solven.cleanthat.github.IGitRefsConstants;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubPRCodeProvider extends AGithubSha1CodeProvider
		implements IListOnlyModifiedFiles, ICodeProviderWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubPRCodeProvider.class);

	final String eventKey;

	final GHPullRequest pr;

	public GithubPRCodeProvider(Path repositoryRoot, String token, String eventKey, GHPullRequest pr) {
		super(repositoryRoot, token, pr.getRepository());
		this.eventKey = eventKey;

		this.pr = pr;
	}

	@Override
	public String getSha1() {
		return pr.getHead().getSha();
	}

	@Override
	public String getRef() {
		return IGitRefsConstants.BRANCHES_PREFIX + pr.getHead().getRef();
	}

	@Override
	public void listFilesForContent(Set<String> includePatterns, Consumer<ICodeProviderFile> consumer)
			throws IOException {
		// If some files are reverted in the PR< they would not be listed, while they may be in diff in the previous
		// head sha1
		pr.listFiles().forEach(prFile -> {
			if ("deleted".equals(prFile.getStatus())) {
				LOGGER.debug("Skip a deleted file: {}", prFile.getFilename());
			} else {
				Path contentPath = CleanthatPathHelpers.makeContentPath(getRepositoryRoot(), prFile.getFilename());
				consumer.accept(new DummyCodeProviderFile(contentPath, prFile));
			}
		});
	}

	public static String loadContent(GHPullRequest pr, String filename) throws IOException {
		GHRepository repository = pr.getRepository();
		String sha1 = pr.getHead().getSha();
		return loadContent(repository, filename, sha1);
	}

	@Override
	public String toString() {
		return pr.getHtmlUrl().toExternalForm();
	}

	@Override
	public boolean persistChanges(Map<Path, String> pathToMutatedContent, ICodeWritingMetadata codeWritingMetadata) {
		GHRepository repo = pr.getRepository();
		var fullRefName = getRef();

		GHRef ref;
		try {
			ref = new GithubRepositoryFacade(repo).getRef(fullRefName);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue fetching refName=" + fullRefName, e);
		}
		GithubRefWriterLogic refWriterLogic = new GithubRefWriterLogic(eventKey, repo, ref, getSha1());
		return refWriterLogic.persistChanges(pathToMutatedContent, codeWritingMetadata);
	}

	@Override
	public Optional<String> loadContentForPath(Path path) throws IOException {
		try {
			var rawPath = CleanthatPathHelpers.makeContentRawPath(getRepositoryRoot(), path);
			return Optional.of(loadContent(pr.getRepository(), rawPath, pr.getHead().getSha()));
		} catch (GHFileNotFoundException e) {
			LOGGER.trace("We miss: {}", path, e);
			LOGGER.debug("We miss: {}", path);
			return Optional.empty();
		}
	}

	@Override
	public String getRepoUri() {
		return pr.getRepository().getGitTransportUrl();
	}

	@Override
	public void cleanTmpFiles() {
		LOGGER.info("Nothing to delete for {}", this);
	}
}
