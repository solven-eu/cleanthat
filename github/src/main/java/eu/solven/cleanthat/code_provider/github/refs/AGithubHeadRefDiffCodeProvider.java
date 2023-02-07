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

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;
import java.nio.file.FileSystem;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public abstract class AGithubHeadRefDiffCodeProvider extends AGithubDiffCodeProvider
		implements IListOnlyModifiedFiles, ICodeProviderWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AGithubHeadRefDiffCodeProvider.class);

	final GHRef head;

	public AGithubHeadRefDiffCodeProvider(FileSystem fs, String token, GHRepository baseRepository, GHRef head) {
		super(fs, token, baseRepository);
		this.head = head;
	}

	/**
	 * head refName, starting with 'refs/'
	 */
	@Override
	protected String getHeadId() {
		// TODO We probably wants to compute a compare between specific sha1, and write into a specific ref. Hence, even
		// if a ref see its head changing, we will always process the same set of files: it helps reproducibility.
		// BEWARE, when loading files, we should load the very latest commit, and not the sha1 commit, else we may
		// remove intermediate commits
		// TODO If case of interlacing commits, should we drop our cleaning, and restart given updated head? Hence the
		// importance of remembering the processed head, and checking this head is still the head when pushing
		return head.getRef();
	}

	@Override
	public void persistChanges(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels) {
		new GithubRefWriterLogic(baseRepository, head).persistChanges(pathToMutatedContent, prComments, prLabels);
	}

	@Override
	public void cleanTmpFiles() {
		LOGGER.info("Nothing to delete for {}", this);
	}

}
