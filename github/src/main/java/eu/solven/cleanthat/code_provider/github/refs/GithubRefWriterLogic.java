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

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriterLogic;
import eu.solven.cleanthat.codeprovider.ICodeWritingMetadata;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link ICodeProviderWriterLogic}
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class GithubRefWriterLogic implements ICodeProviderWriterLogic {

	final String eventKey;

	final GHRepository repo;
	// ref when we started processing the event
	final GHRef target;
	// sha1 of the event
	final String readSha1;

	/**
	 * 
	 * @param readSha1
	 *            the sha1 used to load content before cleaning. It may not be the head of the ref, if some commit has
	 *            been written in the meantime
	 * @param eventKey
	 * @param repo
	 * @param target
	 *            the refs into which we want to commit this data
	 */
	public GithubRefWriterLogic(String eventKey, GHRepository repo, GHRef target, String readSha1) {
		this.eventKey = eventKey;

		this.repo = repo;
		this.target = target;
		this.readSha1 = readSha1;

		String freshTargetSha1 = target.getObject().getSha();
		if (!freshTargetSha1.equals(readSha1)) {
			// Happens if a commit is pushed after the original event (which may be retried much later)
			LOGGER.warn("Target '{}' has been updated {} -> {} (between the event generation and its consumption)",
					target.getRef(),
					readSha1,
					freshTargetSha1);
		}
	}

	@Override
	public boolean persistChanges(Map<Path, String> pathToMutatedContent, ICodeWritingMetadata codeWritingMetadata) {
		return commitIntoRef(pathToMutatedContent, codeWritingMetadata.getComments());
	}

	protected boolean commitIntoRef(Map<Path, String> pathToMutatedContent, List<String> prComments) {
		if (pathToMutatedContent.isEmpty()) {
			LOGGER.info("There is not a single path to write");
			return false;
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
		String veryFreshTargetSha = updatedTarget.getObject().getSha();
		String freshTargetSha1 = target.getObject().getSha();

		if (!veryFreshTargetSha.equals(freshTargetSha1)) {
			// Happens if a commit is pushed during the cleaning
			LOGGER.warn("Target '{}' has been updated {} -> {} (during the event processing)",
					refName,
					freshTargetSha1,
					veryFreshTargetSha);

			// This would be rejected in `doCommitContent` as we do not force push, and the built tree is not a
			// descendant of the new head
		}

		var sha1ToConsiderAsHead = freshTargetSha1;

		var pathToCommitableContent = filterOutPathsHavingDiverged(pathToMutatedContent, refName, sha1ToConsiderAsHead);

		if (pathToCommitableContent.isEmpty()) {
			LOGGER.warn("Due to ref update, there is not a single file to commit");
			return false;
		}

		try {
			return doCommitContent(prComments, repoName, refName, sha1ToConsiderAsHead, pathToCommitableContent);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private boolean doCommitContent(List<String> prComments,
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
			return true;
		} catch (IOException e) {
			throw new UncheckedIOException("The ref has been updated in the meantime?", e);
		}
	}

	/**
	 * This is some sort of merge algorithm. We'd better not re-inventing the wheel, but we want to manage the most
	 * trivial case (e.g. a commit has modified unrelated pathes).
	 * 
	 * @param pathToMutatedContent
	 * @param refName
	 * @param refTargetSha
	 * @return
	 */
	protected Map<Path, String> filterOutPathsHavingDiverged(Map<Path, String> pathToMutatedContent,
			String refName,
			String refTargetSha) {
		if (pathToMutatedContent.isEmpty()) {
			return pathToMutatedContent;
		}

		var fs = pathToMutatedContent.keySet().iterator().next().getFileSystem();

		Map<Path, String> pathToCommitableContent = new LinkedHashMap<>(pathToMutatedContent);
		{
			GHCompare compareContentWithHead;
			try {
				compareContentWithHead = repo.getCompare(readSha1, refTargetSha);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue comparing " + readSha1 + " with " + refTargetSha, e);
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
				String concurrentSha = committedFile.getSha();

				String filename = committedFile.getFileName();
				Path currentAsPath = CleanthatPathHelpers.makeContentPath(fs, filename);
				if (null != pathToCommitableContent.remove(currentAsPath)) {
					LOGGER.warn("We discarded commit of clean file given a concurrent change: {} (sha={})",
							filename,
							concurrentSha);
				} else {
					String previousFilename = committedFile.getPreviousFilename();
					if (previousFilename != null) {
						Path previousAsPath = CleanthatPathHelpers.makeContentPath(fs, previousFilename);
						if (null != pathToCommitableContent.remove(previousAsPath)) {
							LOGGER.warn("We discarded commit of clean file given a concurrent change: {} (sha={})",
									previousFilename,
									concurrentSha);
						}
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
