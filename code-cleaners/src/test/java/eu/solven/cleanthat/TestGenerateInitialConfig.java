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
package eu.solven.cleanthat;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.GenerateInitialConfig;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class TestGenerateInitialConfig {
	@Test
	public void testGenerateDefaultConfig_empty() throws IOException {
		IEngineLintFixerFactory factory = Mockito.mock(IEngineLintFixerFactory.class);
		GenerateInitialConfig generator = new GenerateInitialConfig(Arrays.asList(factory));

		ICodeProvider codeProvider = Mockito.mock(ICodeProvider.class);
		CleanthatRepositoryProperties config = generator.prepareDefaultConfiguration(codeProvider).getRepoProperties();

		Assertions.assertThat(config.getSourceCode().getIncludes()).isEmpty();
		Assertions.assertThat(config.getSourceCode().getExcludes()).isEmpty();
	}

	@Test
	public void testGenerateDefaultConfig_mvnWrapper() throws IOException {
		IEngineLintFixerFactory factory = Mockito.mock(IEngineLintFixerFactory.class);
		GenerateInitialConfig generator = new GenerateInitialConfig(Arrays.asList(factory));

		ICodeProvider codeProvider = Mockito.mock(ICodeProvider.class);
		Mockito.when(codeProvider.loadContentForPath("/.mvn/wrapper/maven-wrapper.properties"))
				.thenReturn(Optional.of("somePropertiesFileContent"));

		CleanthatRepositoryProperties config = generator.prepareDefaultConfiguration(codeProvider).getRepoProperties();

		Assertions.assertThat(config.getSourceCode().getIncludes()).isEmpty();
		Assertions.assertThat(config.getSourceCode().getExcludes()).isEmpty();
		// Assertions.assertThat(config.getSourceCode().getExcludes()).hasSize(1).contains("glob:/.mvn/wrapper/**");
	}
}
