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
package eu.solven.cleanthat.engine.java.eclipse;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

public class TestEclipseJavaFormatterConfiguration {
	final ICodeProvider codeProvider = Mockito.mock(ICodeProvider.class);
	final CleanthatEngineProperties engineProperties = CleanthatEngineProperties.builder().build();
	final EclipseJavaFormatterProcessorProperties processorConfig = new EclipseJavaFormatterProcessorProperties();

	@Test
	public void testLoadConfig_empty() {
		processorConfig.setUrl("");

		EclipseJavaFormatterConfiguration config =
				EclipseJavaFormatterConfiguration.load(codeProvider, engineProperties, processorConfig);

		Assertions.assertThat(config.getSettings())
				.hasSize(3)
				.containsEntry(JavaCore.COMPILER_SOURCE, "1.8")
				.containsEntry(JavaCore.COMPILER_COMPLIANCE, "1.8")
				.containsEntry(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.8");
	}

	@Test
	public void testLoadConfig_default() {
		EclipseJavaFormatterConfiguration config =
				EclipseJavaFormatterConfiguration.load(codeProvider, engineProperties, processorConfig);

		Assertions.assertThat(config.getSettings()).hasSize(332)
		// .containsEntry(JavaCore.COMPILER_SOURCE, "0")
		// .containsEntry(JavaCore.COMPILER_COMPLIANCE, "0")
		// .containsEntry(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "0")
		;
	}

	@Test
	public void testLoadConfig_fromFile() throws IOException {
		String path = "/config/eclipse_java_code_formatter.xml";

		processorConfig.setUrl("code:" + path);

		String content =
				new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		Mockito.when(codeProvider.loadContentForPath(path)).thenReturn(Optional.of(content));

		EclipseJavaFormatterConfiguration config =
				EclipseJavaFormatterConfiguration.load(codeProvider, engineProperties, processorConfig);

		Assertions.assertThat(config.getSettings())
				.hasSize(308)
				// .containsEntry(JavaCore.COMPILER_SOURCE, IJdkVersionConstants.JDK_8)
				// .containsEntry(JavaCore.COMPILER_COMPLIANCE, IJdkVersionConstants.JDK_8)
				.containsEntry(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.8");
	}
}
