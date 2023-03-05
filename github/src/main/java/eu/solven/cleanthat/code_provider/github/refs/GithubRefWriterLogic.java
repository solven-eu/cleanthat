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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriterLogic;
import eu.solven.cleanthat.codeprovider.ICodeWritingMetadata;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;

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
	final GHRef target;
	final String headSha1;

	/**
	 * 
	 * @param headSha1
	 *            the sha1 used to load content before cleaning. It may not be the head of the ref, if some commit has
	 *            been written in the meantime
	 * @param eventKey
	 * @param repo
	 * @param target
	 *            the refs into which we want to commit this data
	 */
	public GithubRefWriterLogic(String eventKey, GHRepository repo, GHRef target, String headSha1) {
		this.eventKey = eventKey;

		this.repo = repo;
		this.target = target;
		this.headSha1 = headSha1;
	}

	@Override
	public void persistChanges(Map<Path, String> pathToMutatedContent, ICodeWritingMetadata codeWritingMetadata) {
		commitIntoRef(pathToMutatedContent, codeWritingMetadata.getComments());
	}

	protected void commitIntoRef(Map<Path, String> pathToMutatedContent, List<String> prComments) {
		if (pathToMutatedContent.isEmpty()) {
			LOGGER.info("There is not a single path to write");
			return;
		}

		String repoName = repo.getFullName();
		String refName = target.getRef();
		LOGGER.debug("Persisting into {}:{}", repoName, refName);

		GHRef updatedTarget;
		try {
			updatedTarget = repo.getRef(target.getRef());
		} catch (IOException e) {
			throw new UncheckedIOException("Issue fetching updated " + refName, e);
		}

		// TODO What if the ref has moved to another sha1 in the meantime?
		// We should not commit directly in the branch, but commit in a tmp branch, and merge it right away
		// Then, in case of conflicts (e.g. due to event being processed very lately, or race-condition), we would just
		// drop some events/paths
		String refTargetSha = updatedTarget.getObject().getSha();

		String oldTargetSha1 = target.getObject().getSha();
		if (!refTargetSha.equals(oldTargetSha1)) {
			// Happens if a commit is pushed during the cleaning
			LOGGER.warn("Target '{}' has been updated {} -> {}", refName, oldTargetSha1, refTargetSha);
		}
		if (!refTargetSha.equals(headSha1)) {
			// Happens if a commit is pushed after the original event (which may be retried much later)
			LOGGER.warn("Target '{}' has been updated {} -> {}", refName, oldTargetSha1, headSha1);
		}

		var pathToCommitableContent = filterOutPathsHavingDiverged(pathToMutatedContent, refName, refTargetSha);

		if (pathToCommitableContent.isEmpty()) {
			LOGGER.warn("Due to ref update, there is not a single file to commit");
			return;
		}

		try {
			doCommitContent(prComments, repoName, refName, refTargetSha, pathToCommitableContent);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void doCommitContent(List<String> prComments,
			String repoName,
			String refName,
			String refTargetSha,
			Map<Path, String> pathToCommitableContent) throws IOException {
		GHTreeBuilder createTree = prepareBuilderTree(repo, pathToCommitableContent);
		GHTree createdTree = createTree.baseTree(refTargetSha).create();

		List<String> allCommitRows = new ArrayList<>();
		allCommitRows.addAll(prComments);
		allCommitRows.add("eventKey: " + eventKey);

		var commitMessage = allCommitRows.stream().collect(Collectors.joining(CodeProviderFormatter.EOL));
		GHCommitBuilder preparedCommit =
				prepareCommit(repo).message(commitMessage).parent(refTargetSha).tree(createdTree.getSha());

		computeSignature().ifPresent(s -> preparedCommit.withSignature(s));

		GHCommit commit = preparedCommit.create();

		String newHead = commit.getSHA1();
		LOGGER.info("Update {} files in {}:{} to {} ({})",
				pathToCommitableContent.size(),
				repoName,
				refName,
				newHead,
				commit.getHtmlUrl());

		try {
			// https://docs.github.com/en/rest/git/refs?apiVersion=2022-11-28#update-a-reference
			target.updateTo(newHead);
		} catch (IOException e) {
			throw new UncheckedIOException("The ref has been updated in the meantime?", e);
		}
	}

	private Map<Path, String> filterOutPathsHavingDiverged(Map<Path, String> pathToMutatedContent,
			String refName,
			String refTargetSha) {
		if (pathToMutatedContent.isEmpty()) {
			return pathToMutatedContent;
		}

		var root = pathToMutatedContent.keySet().iterator().next();

		Map<Path, String> pathToCommitableContent = new LinkedHashMap<>(pathToMutatedContent);
		{
			GHCompare compareContentWithHead;
			try {
				compareContentWithHead = repo.getCompare(headSha1, refTargetSha);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue comparing " + headSha1 + " with " + refTargetSha, e);
			}

			int aheadBy = compareContentWithHead.getAheadBy();
			if (aheadBy > 0) {
				// Some commits has been pushed in the meantime
				LOGGER.warn("Target '{}' is ahead by {} commits", refName, aheadBy);
			}

			int behindBy = compareContentWithHead.getBehindBy();
			if (behindBy > 0) {
				// The ref has been forced push to a commit in the past, even before the head of the event?
				LOGGER.error("Target '{}' is  behind by {}", refName, behindBy);
			}
			LOGGER.info("The cleaned sha1 status is {}", compareContentWithHead.getStatus());

			// We clean a head, given the diff-set of files compared to a base.
			// However, the head may be quite old, and some other commits may have been pushed onto the ref
			Stream.of(compareContentWithHead.getFiles()).forEach(committedFile -> {
				String previousFilename = committedFile.getPreviousFilename();
				String concurrentSha = committedFile.getSha();
				if (previousFilename != null) {
					if (null != pathToCommitableContent.remove(root.resolve(previousFilename))) {
						LOGGER.warn("We discarded commit of clean file given a concurrent change: {} (sha={})",
								committedFile,
								concurrentSha);
					}
				} else {
					String filename = committedFile.getFileName();
					if (null != pathToCommitableContent.remove(root.resolve(filename))) {
						LOGGER.warn("We discarded commit of clean file given a concurrent change: {} (sha={})",
								filename,
								concurrentSha);
					}
				}
			});
		}
		return pathToCommitableContent;
	}

	public static GHTreeBuilder prepareBuilderTree(GHRepository repo, Map<Path, String> pathToMutatedContent) {
		GHTreeBuilder createTree = repo.createTree();
		pathToMutatedContent.forEach((path, content) -> {
			CleanthatPathHelpers.checkContentPath(path);

			// TODO isExecutable isn't a parameter from the original file?
			createTree.add(path.toString(), content, false);
		});
		return createTree;
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
