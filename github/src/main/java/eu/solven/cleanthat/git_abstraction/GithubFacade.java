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
package eu.solven.cleanthat.git_abstraction;

import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enable a Facade over RewiewRequestProvider
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubFacade {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubFacade.class);

	final GitHub github;
	final String repoName;

	final GithubRepositoryFacade repoFacade;

	public GithubFacade(GitHub github, String repoName) throws IOException {
		this.github = github;
		this.repoName = repoName;

		this.repoFacade = new GithubRepositoryFacade(github.getRepository(repoName));
	}

	public static String toFullGitRef(GHCommitPointer ghCommitPointer) {
		String githubRef = ghCommitPointer.getRef();
		return branchToRef(githubRef);
	}

	public static String branchToRef(String branchName) {
		if (branchName.startsWith(CleanthatRefFilterProperties.BRANCHES_PREFIX)) {
			LOGGER.warn("We expected a branchName, not a ref: {}",
					branchName,
					new RuntimeException("We want a stackTrace"));
			return branchName;
		}

		return CleanthatRefFilterProperties.BRANCHES_PREFIX + branchName;
	}

	@Deprecated
	public Stream<GHPullRequest> findAnyPrHeadMatchingRef(String ref) throws IOException {
		return repoFacade.findAnyPrHeadMatchingRef(ref);
	}

	@Deprecated
	public Optional<GHPullRequest> findFirstPrBaseMatchingRef(String ref) throws IOException {
		return repoFacade.findFirstPrBaseMatchingRef(ref);
	}

	@Deprecated
	public Optional<?> openPrIfNoneExists(GitRepoBranchSha1 base, GitRepoBranchSha1 head, String title, String body)
			throws IOException {
		return repoFacade.openPrIfNoneExists(base, head, title, body);
	}

	@Deprecated
	public void removeRef(GitRepoBranchSha1 ref) throws IOException {
		repoFacade.removeRef(ref);
	}

	@Deprecated
	public GHRef getRef(String refName) throws IOException {
		return repoFacade.getRef(refName);
	}

	public GHRepository getRepository() {
		return repoFacade.getRepository().getDecorated();
	}

}
