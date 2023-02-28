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
package eu.solven.cleanthat.config;

import java.io.IOException;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.mockito.Mockito;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.code_provider.inmemory.FileSystemCodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;

public class TestCleanthatConfigInitializer {
	@Test
	public void testGenerate() throws IOException {
		ICodeProvider codeProvider = FileSystemCodeProvider.forTests();
		IEngineLintFixerFactory factory = Mockito.mock(IEngineLintFixerFactory.class);
		var initializer = new CleanthatConfigInitializer(ConfigHelpers.makeYamlObjectMapper(), Arrays.asList(factory));

		var result = initializer.prepareFile(codeProvider, false);

		Assertions.assertThat(result.getPrBody()).contains("Cleanthat").doesNotContain("$");
		Assertions.assertThat(result.getCommitMessage()).contains("Cleanthat");
		var root = codeProvider.getRepositoryRoot();
		Assertions.assertThat(result.getPathToContents())
				.hasSize(1)
				.containsKey(CleanthatPathHelpers.makeContentPath(root, ".cleanthat/cleanthat.yaml"))
				.hasValueSatisfying(new Condition<String>(v -> v.contains("syntax_version: \"2023-01-09\""), ""));
	}
}
