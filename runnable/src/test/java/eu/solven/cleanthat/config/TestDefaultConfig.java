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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.language.spotless.CleanthatSpotlessProperties;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class TestDefaultConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestDefaultConfig.class);

	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(CleanthatRepositoryProperties.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testFromJsonToYaml() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();

		CleanthatRepositoryProperties safeRebuiltFromEmpty =
				yamlObjectMapper.convertValue(Map.of(), CleanthatRepositoryProperties.class);

		// By in safe-default, we exclude anything in an 'exclude' directory
		{
			Assertions.assertThat(safeRebuiltFromEmpty.getSourceCode().getExcludes()).isEmpty();
			safeRebuiltFromEmpty.getSourceCode().setExcludes(Arrays.asList("regex:.*/generated/.*"));
		}

		{
			Assertions.assertThat(safeRebuiltFromEmpty.getEngines()).isEmpty();
			// Ensure mutability
			safeRebuiltFromEmpty.setEngines(new ArrayList<>());

			// TODO Refactor with
			// eu.solven.cleanthat.config.GenerateInitialConfig.prepareDefaultConfiguration(ICodeProvider)
			{
				CleanthatEngineProperties engineProperties = new CleanthatEngineProperties();

				engineProperties.setEngine("spotless");
				SourceCodeProperties javaSourceCodeProperties = new SourceCodeProperties();
				javaSourceCodeProperties
						.setIncludes(Arrays.asList("regex:.*\\.java", "regex:.*\\.json", "glob:**/pom.xml"));
				engineProperties.setSourceCode(javaSourceCodeProperties);

				Assertions.assertThat(engineProperties.getSteps()).isEmpty();
				engineProperties.setSteps(new ArrayList<>());

				engineProperties.getSteps()
						.add(CleanthatStepProperties.builder()
								.id("spotless")
								.parameters(new CleanthatSpotlessProperties())
								.build());

				safeRebuiltFromEmpty.getEngines().add(engineProperties);
			}
		}

		// This is useful to convert the Java class of processors into Map (like it will happen when loading from the
		// yaml)
		String defaultConfigAsYaml = yamlObjectMapper.writeValueAsString(safeRebuiltFromEmpty);
		LOGGER.info("Default config as YAML: {}{}", System.lineSeparator(), defaultConfigAsYaml);
		CleanthatRepositoryProperties configFromEmptyAsMap =
				yamlObjectMapper.readValue(defaultConfigAsYaml, CleanthatRepositoryProperties.class);

		ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(yamlObjectMapper));
		CleanthatRepositoryProperties configDefaultSafe =
				configHelpers.loadRepoConfig(new ClassPathResource("/config/default-safe.yaml"));

		Assert.assertEquals(configDefaultSafe.toString(), configFromEmptyAsMap.toString());
		Assert.assertEquals(configDefaultSafe, configFromEmptyAsMap);
	}
}
