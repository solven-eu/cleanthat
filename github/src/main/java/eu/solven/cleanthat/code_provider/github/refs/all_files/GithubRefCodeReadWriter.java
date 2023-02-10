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

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubSha1CodeProviderWriter;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import java.nio.file.FileSystem;
import java.util.Objects;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubRefCodeReadWriter extends AGithubSha1CodeProviderWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubRefCodeReadWriter.class);

	final GHRef ref;
	final String sha1;

	public GithubRefCodeReadWriter(FileSystem fs,
			String token,
			String eventKey,
			GHRepository repo,
			GHRef ref,
			String sha1) {
		super(fs, token, eventKey, repo);

		this.ref = Objects.requireNonNull(ref, "ref is null");
		this.sha1 = sha1;

		if (!this.sha1.equals(ref.getObject().getSha())) {
			// This typically happens when executing an old event
			LOGGER.debug("We are considering writing into a ref, given a (read) sha1 which is not the head");
		}
	}

	@Override
	public String getSha1() {
		return sha1;
	}

	@Override
	public String getRef() {
		return ref.getRef();
	}

	@Override
	protected GHRef getAsGHRef() {
		return ref;
	}

	@Override
	public String toString() {
		return ref.getUrl().toExternalForm();
	}

}
