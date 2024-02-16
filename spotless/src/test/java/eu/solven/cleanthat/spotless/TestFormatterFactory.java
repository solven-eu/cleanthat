/*
 * Copyright 2024 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.spotless;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;

public class TestFormatterFactory {
	final ICodeProvider codeProvider = Mockito.mock(ICodeProvider.class);
	final SpotlessFormatterProperties formatterProperties =
			SpotlessFormatterProperties.builder().format("java").build();

	@Test
	public void testToggleOffOn() {
		CleanthatSession cleanthatSession = CleanthatSession.builder().codeProvider(codeProvider).build();
		FormatterFactory factory = new FormatterFactory(cleanthatSession);

		AFormatterFactory langFormatterFactory = factory.makeFormatterFactory(formatterProperties);
		AFormatterStepFactory javaFormatterFactory = factory.makeFormatterStepFactory(formatterProperties);

		final SpotlessFormatterProperties propertiesWithSteps =
				SpotlessFormatterProperties.builder().format("java").steps(langFormatterFactory.exampleSteps()).build();

		List<FormatterStep> steps =
				factory.buildSteps(javaFormatterFactory, propertiesWithSteps, Mockito.mock(Provisioner.class));

		Assertions.assertThat(steps.get(0).getName()).isEqualTo("toggleIn");

		Assertions.assertThat(steps.get(steps.size() - 1).getName()).isEqualTo("toggleOut");
	}
}
