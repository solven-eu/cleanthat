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
package eu.solven.cleanthat.config.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.lambda.TechnicalBoilerplateSpringConfig;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * Set of beans related to reading/writing CleanThat configuration
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import(TechnicalBoilerplateSpringConfig.class)
public class ConfigSpringConfig {

	// Primary as most situations to write something is to write JSON
	@Bean
	@Primary
	@Qualifier("json")
	public ObjectMapper jsonObjectMapper() {
		return ConfigHelpers.makeJsonObjectMapper();
	}

	// YAML is still very useful to read configuration
	@Bean
	@Qualifier("yaml")
	public ObjectMapper yamlObjectMapper() {
		return ConfigHelpers.makeYamlObjectMapper();
	}

	@Bean
	public ConfigHelpers configHelpers(List<ObjectMapper> objectMappers) {
		return new ConfigHelpers(objectMappers);
	}

}
