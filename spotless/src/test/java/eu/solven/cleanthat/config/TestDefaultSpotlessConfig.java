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
package eu.solven.cleanthat.config;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessEngineProperties;
import java.io.IOException;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class TestDefaultSpotlessConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestDefaultSpotlessConfig.class);

	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(CleanthatRepositoryProperties.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testDefaultSpotless() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();

		SpotlessEngineProperties safeRebuiltFromEmpty = SpotlessEngineProperties.defaultEngineWithMarkdown();

		// By default, we format markdown, as we expect most repository to have a README.MD
		{
			Assertions.assertThat(safeRebuiltFromEmpty.getFormatters())
					.hasSize(1)
					.singleElement()
					.matches(p -> p.getFormat().equals("markdown"));
		}

		// This is useful to convert the Java class of processors into Map (like it will happen when loading from the
		// yaml)
		String defaultConfigAsYaml = yamlObjectMapper.writeValueAsString(safeRebuiltFromEmpty);
		LOGGER.info("Default config as YAML: {}{}", System.lineSeparator(), defaultConfigAsYaml);
		SpotlessEngineProperties safeConfigFromYaml =
				yamlObjectMapper.readValue(defaultConfigAsYaml, SpotlessEngineProperties.class);

		SpotlessEngineProperties configDefaultSafe =
				yamlObjectMapper.readValue(new ClassPathResource("/config/default-spotless.yaml").getInputStream(),
						SpotlessEngineProperties.class);

		Assert.assertEquals(yamlObjectMapper.writeValueAsString(configDefaultSafe),
				yamlObjectMapper.writeValueAsString(safeConfigFromYaml));
		Assert.assertEquals(configDefaultSafe, safeConfigFromYaml);
	}
}
