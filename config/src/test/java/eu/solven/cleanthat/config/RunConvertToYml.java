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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class RunConvertToYml {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunConvertToYml.class);

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		var jsonObjectMapper = ConfigHelpers.makeJsonObjectMapper();
		var configHelpers = new ConfigHelpers(Arrays.asList(jsonObjectMapper));

		var config = configHelpers.loadRepoConfig(new FileSystemResource("../cleanthat.json"));

		var yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();
		var asYaml = yamlObjectMapper.writeValueAsString(config);

		LOGGER.info("Config as yaml: {}{}{}{}{}{}",
				System.lineSeparator(),
				"------",
				System.lineSeparator(),
				asYaml,
				System.lineSeparator(),
				"------");
	}
}
