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
package eu.solven.cleanthat.engine;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.formatter.LineEnding;

public class TestSourceCodeProperties {
	final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testDefaultConstructor() {
		SourceCodeProperties properties = SourceCodeProperties.builder().build();

		// We rely on null so that any other parameter takes precedence
		Assertions.assertThat(properties.getLineEndingAsEnum()).isNull();
	}

	@Test
	public void testDefaultMethod() throws JsonMappingException, JsonProcessingException {
		SourceCodeProperties properties = SourceCodeProperties.defaultRoot();

		// By default, neither LR or CRLF as we should not privilege a platform
		Assertions.assertThat(properties.getLineEndingAsEnum()).isEqualTo(LineEnding.GIT);

		String asString = objectMapper.writeValueAsString(properties);
		Assertions.assertThat(asString).contains("line_ending", "GIT");

		SourceCodeProperties asObject = objectMapper.readValue(asString, SourceCodeProperties.class);

		Assertions.assertThat(asObject).isEqualTo(properties);
	}
}
