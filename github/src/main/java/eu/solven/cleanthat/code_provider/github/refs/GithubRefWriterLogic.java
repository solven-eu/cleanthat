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

import eu.solven.cleanthat.codeprovider.ICodeProviderWriterLogic;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link ICodeProviderWriterLogic}
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubRefWriterLogic implements ICodeProviderWriterLogic {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubRefWriterLogic.class);

	final String eventKey;

	final GHRepository repo;
	final GHRef head;

	public GithubRefWriterLogic(String eventKey, GHRepository repo, GHRef head) {
		this.eventKey = eventKey;
		this.repo = repo;
		this.head = head;
	}

	@Override
	public void persistChanges(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels) {
		commitIntoRef(pathToMutatedContent, prComments, head);
	}

	protected void commitIntoRef(Map<String, String> pathToMutatedContent, List<String> prComments, GHRef ref) {
		String repoName = repo.getFullName();
		String refName = ref.getRef();
		LOGGER.debug("Persisting into {}:{}", repoName, refName);

		String refSha = ref.getObject().getSha();
		Map<String, GHCompare> contentShaToCompare = new LinkedHashMap<>();

		GHTreeBuilder createTree = repo.createTree();
		pathToMutatedContent.forEach((path, content) -> {
			if (!path.startsWith("/")) {
				throw new IllegalStateException("We expect to receive only rooted path: " + path);
			}

			// Remove the leading '/'
			path = path.substring("/".length());

			try {
				GHContent latestContent = repo.getFileContent(path, refName);

				GHCompare compareContentWithHead =
						contentShaToCompare.computeIfAbsent(latestContent.getSha(), contentSha -> {
							try {
								return repo.getCompare(refSha, contentSha);
							} catch (IOException e) {
								throw new UncheckedIOException("Issue comparing " + refSha + " with " + contentSha, e);
							}
						});

				int aheadBy = compareContentWithHead.getAheadBy();
				if (aheadBy > 0) {
					LOGGER.info("'{}' is ahead by {}", path, aheadBy);
				}

				int behindBy = compareContentWithHead.getBehindBy();
				if (behindBy > 0) {
					LOGGER.info("'{}' is behind by {}", path, behindBy);
				}
				LOGGER.info("'{}' status is {}", path, compareContentWithHead.getStatus());
			} catch (IOException e) {
				throw new UncheckedIOException("Issue fetching content for path=" + path, e);
			}

			// TODO isExecutable isn't a parameter from the original file?
			createTree.add(path, content, false);
		});

		// TODO What if the ref has moved to another sha1 in the meantime?
		// We should not commit directly in the branch, but commit in a tmp branch, and merge it right away
		// Then, in case of conflicts (e.g. due to event being processed very lately, or race-condition), we would just
		// drop some events/paths
		String sha = ref.getObject().getSha();

		try {
			GHTree createdTree = createTree.baseTree(sha).create();

			List<String> allCommitRows = new ArrayList<>();
			allCommitRows.addAll(prComments);
			allCommitRows.add("eventKey: " + eventKey);

			String commitMessage = allCommitRows.stream().collect(Collectors.joining(CodeProviderFormatter.EOL));
			GHCommitBuilder preparedCommit =
					prepareCommit(repo).message(commitMessage).parent(sha).tree(createdTree.getSha());

			computeSignature().ifPresent(s -> preparedCommit.withSignature(s));

			GHCommit commit = preparedCommit.create();

			String newHead = commit.getSHA1();
			LOGGER.info("Update {}:{} to {} ({})", repoName, refName, newHead, commit.getHtmlUrl());

			ref.updateTo(newHead);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits
	// https://github.com/GitbookIO/github-api-signature/blob/master/src/createSignature.ts#L34
	protected Optional<String> computeSignature() {
		return Optional.empty();
	}

	public static GHCommitBuilder prepareCommit(GHRepository repo) {
		return repo.createCommit()
		// https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification#signature-verification-for-bots
		// No author so that the commit is automatically marked as Verified by Github
		// .author("CleanThat", "CleanThat", new Date())
		;
	}
}
