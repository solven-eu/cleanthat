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
package eu.solven.cleanthat.code_provider.github.code_provider;

import eu.solven.cleanthat.code_provider.github.refs.GithubRefWriterLogic;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import java.nio.file.FileSystem;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public abstract class AGithubSha1CodeProviderWriter extends AGithubSha1CodeProvider
		implements ICodeProviderWriter, IGithubSha1CodeProvider {
	// private static final Logger LOGGER = LoggerFactory.getLogger(AGithubSha1CodeProviderWriter.class);

	public AGithubSha1CodeProviderWriter(FileSystem fs, String token, GHRepository repo) {
		super(fs, token, repo);
	}

	protected abstract GHRef getAsGHRef();

	@Override
	public void persistChanges(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels) {
		new GithubRefWriterLogic(repo, getAsGHRef()).persistChanges(pathToMutatedContent, prComments, prLabels);
	}

	@Override
	public void cleanTmpFiles() {
		helper.cleanTmpFiles();
	}

}
