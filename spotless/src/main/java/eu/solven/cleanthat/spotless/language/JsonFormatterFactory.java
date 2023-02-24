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
package eu.solven.cleanthat.spotless.language;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;

/**
 * Configure Spotless engine for '.json' files
 * 
 * @author Benoit Lacelle
 *
 */
public class JsonFormatterFactory extends AFormatterFactory {
	// We consider any json to be cleaned. This is more an example template than a 100%
	// guarantee rules. One may prefer '**/src/**/*.json'
	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.of("**/*.json");

	@Override
	public Set<String> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	@Override
	public JsonFormatterStepFactory makeStepFactory(ICodeProvider codeProvider,
			SpotlessFormatterProperties formatterProperties) {
		return new JsonFormatterStepFactory(this, codeProvider, formatterProperties);
	}

	// This is useful to demonstrate all available configuration
	@Override
	public List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties jackson = SpotlessStepProperties.builder().id("jackson").build();
		SpotlessStepParametersProperties jacksonParameters = new SpotlessStepParametersProperties();
		jacksonParameters.putProperty("features",
				ImmutableMap.builder().put(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS.name(), true).build());
		jacksonParameters.putProperty("yaml_features",
				ImmutableMap.builder().put(JsonGenerator.Feature.QUOTE_FIELD_NAMES.name(), false).build());
		jackson.setParameters(jacksonParameters);

		return ImmutableList.<SpotlessStepProperties>builder().add(jackson).build();
	}
}
