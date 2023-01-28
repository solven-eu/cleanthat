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

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubSha1CodeProviderWriter;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubBranchCodeProvider extends AGithubSha1CodeProviderWriter {
	final GHBranch branch;

	public GithubBranchCodeProvider(FileSystem fs, String token, GHRepository repo, GHBranch branch) {
		super(fs, token, repo);
		this.branch = branch;
	}

	@Override
	public String toString() {
		return branch.getOwner().getFullName() + "/" + branch.getName();
	}

	@Override
	public String getSha1() {
		return branch.getSHA1();
	}

	@Override
	public String getRef() {
		return CleanthatRefFilterProperties.BRANCHES_PREFIX + branch.getName();
	}

	@Override
	protected GHRef getAsGHRef() {
		String refName = getRef();
		try {
			return new GithubRepositoryFacade(getRepo()).getRef(refName);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue fetching ref=" + refName, e);
		}
	}

}
