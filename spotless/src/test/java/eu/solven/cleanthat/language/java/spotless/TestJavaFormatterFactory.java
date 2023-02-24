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
package eu.solven.cleanthat.language.java.spotless;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import eu.solven.cleanthat.spotless.IFormatterStepConstants;
import eu.solven.cleanthat.spotless.language.JavaFormatterFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;

public class TestJavaFormatterFactory {
	@Test
	public void testExampleSteps() {
		List<SpotlessStepProperties> exampleSteps = new JavaFormatterFactory().exampleSteps();

		Assertions.assertThat(exampleSteps).anyMatch(r -> "cleanthat".equals(r.getId()) && !r.isSkip());

		// We need to work on providing default files
		Assertions.assertThat(exampleSteps)
				.noneMatch(r -> r.getParameters().getCustomProperties().containsKey(IFormatterStepConstants.KEY_FILE)
						&& !r.isSkip());
	}
}
