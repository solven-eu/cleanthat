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
package eu.solven.cleanthat.code_provider.github.refs.all_files;

import java.nio.file.Path;
import java.util.Objects;

import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubSha1CodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProvider;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubRefCodeProvider extends AGithubSha1CodeProvider {
	final GHRef ref;

	public GithubRefCodeProvider(Path repositoryRoot, String token, GHRepository repo, GHRef ref) {
		super(repositoryRoot, token, repo);

		this.ref = Objects.requireNonNull(ref, "ref is null");
	}

	@Override
	public String getSha1() {
		return ref.getObject().getSha();
	}

	@Override
	public String getRef() {
		return ref.getRef();
	}

	@Override
	public String toString() {
		return ref.getUrl().toExternalForm();
	}

}
