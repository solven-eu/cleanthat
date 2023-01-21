/*
 * Copyright 2023 Solven
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
package eu.solven.cleanthat.code_provider.github.refs.all_files;

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubSha1CodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubCommitCodeProvider extends AGithubSha1CodeProvider {
	final GHCommit commit;

	public GithubCommitCodeProvider(String token, GHRepository repo, GHCommit commit) {
		super(token, repo);
		this.commit = commit;
	}

	@Override
	public String toString() {
		return getRepo().getFullName() + "/" + getSha1();
	}

	@Override
	public String getSha1() {
		return commit.getSHA1();
	}

	@Override
	public String getRef() {
		return getSha1();
	}

}
