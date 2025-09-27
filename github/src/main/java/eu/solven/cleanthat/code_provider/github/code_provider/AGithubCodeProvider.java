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
package eu.solven.cleanthat.code_provider.github.code_provider;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import eu.solven.cleanthat.codeprovider.ICodeProvider;

/**
 * An {@link ICodeProvider} for Github code. Sub-classes manages PR, ref/branches/...
 *
 * @author Benoit Lacelle
 */
public abstract class AGithubCodeProvider implements ICodeProvider {

	final Path repositoryRoot;

	public AGithubCodeProvider(Path repositoryRoot) {
		this.repositoryRoot = repositoryRoot;
	}

	@Override
	public Path getRepositoryRoot() {
		return repositoryRoot;
	}

	public static String loadContent(GHRepository repository, String filename, String sha1) throws IOException {
		GHContent content = repository.getFileContent(filename, sha1);

		if ("none".equals(content.getEncoding())) {
			// https://github.com/hub4j/github-api/issues/1558
			throw new FileIsTooBigException(content.getGitUrl(), content.getSize());
		}

		String asString;
		try (var reader = new InputStreamReader(content.read(), Charsets.UTF_8)) {
			asString = CharStreams.toString(reader);
		}
		return asString;
	}
}
