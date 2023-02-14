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
package eu.solven.cleanthat.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.code_provider.local.FileSystemGitCodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.CleanthatConfigInitializer;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.RepoInitializerResult;
import eu.solven.cleanthat.language.spotless.SpotlessFormattersFactory;

public class TestRepo_GenerateInitiaConfiguration {
	@Test
	public void testGenerateConfiguration() throws IOException {
		Resource srcMainResourcesResource = new ClassPathResource("/application.yml");

		File repositoryRoot = srcMainResourcesResource.getFile();
		while (!new File(repositoryRoot, ".git").isDirectory()) {
			repositoryRoot = repositoryRoot.getParentFile();
		}

		ICodeProvider codeProvider = new FileSystemGitCodeProvider(repositoryRoot.toPath());

		ObjectMapper objectMapper = ConfigHelpers.makeYamlObjectMapper();
		SpotlessFormattersFactory factory =
				new SpotlessFormattersFactory(new ConfigHelpers(Arrays.asList(objectMapper)),
						SpotlessFormattersFactory.makeProvisioner());

		CleanthatConfigInitializer initializer =
				new CleanthatConfigInitializer(codeProvider, objectMapper, Arrays.asList(factory));

		RepoInitializerResult result = initializer.prepareFile(false);

		Assertions.assertThat(result.getPrBody()).contains("Cleanthat").doesNotContain("$");
		Assertions.assertThat(result.getCommitMessage()).contains("Cleanthat");
		Path root = codeProvider.getRepositoryRoot();
		Assertions.assertThat(result.getPathToContents())
				.hasSize(2)
				.containsKey(CleanthatPathHelpers.makeContentPath(root, ".cleanthat/cleanthat.yaml"))
				.hasValueSatisfying(new Condition<String>(v -> v.contains("id: \"spotless\""), ""))
				.containsKey(CleanthatPathHelpers.makeContentPath(root, ".cleanthat/spotless.yaml"))
				.hasValueSatisfying(new Condition<String>(v -> v.contains("format: \"markdown\""), ""));
	}
}
