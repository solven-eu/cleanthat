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

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.json.JacksonJsonStep;
import com.diffplug.spotless.yaml.JacksonYamlConfig;
import com.diffplug.spotless.yaml.JacksonYamlStep;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import java.util.List;
import java.util.Map;

/**
 * Configure Spotless engine for JSON files
 * 
 * @author Benoit Lacelle
 *
 */
public class YamlFormatterStepFactory extends AFormatterStepFactory {
	public YamlFormatterStepFactory(YamlFormatterFactory factory,
			ICodeProvider codeProvider,
			SpotlessFormatterProperties spotlessProperties) {
		super(factory, codeProvider, spotlessProperties);
	}

	@Override
	public String licenseHeaderDelimiter() {
		return null;
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public FormatterStep makeSpecializedStep(SpotlessStepProperties s, Provisioner provisioner) {
		String stepId = s.getId();
		switch (stepId) {
		case "jackson": {
			return makeJackson(s.getParameters(), provisioner);
		}
		default: {
			throw new IllegalArgumentException("Unknown step: " + stepId);
		}
		}
	}

	private FormatterStep makeJackson(SpotlessStepParametersProperties parameters, Provisioner provisioner) {
		JacksonYamlConfig jacksonConfig = new JacksonYamlConfig();

		@SuppressWarnings("unchecked")
		Map<String, Boolean> features = parameters.getCustomProperty("features", Map.class);
		if (features != null) {
			jacksonConfig.appendFeatureToToggle(features);
		}

		@SuppressWarnings("unchecked")
		Map<String, Boolean> yamlFeatures = parameters.getCustomProperty("yaml_features", Map.class);
		if (features != null) {
			jacksonConfig.appendYamlFeatureToToggle(yamlFeatures);
		}

		String version = parameters.getCustomProperty("version", String.class);
		if (version == null) {
			version = JacksonJsonStep.defaultVersion();
		}

		return JacksonYamlStep.create(jacksonConfig, version, provisioner);
	}

	// This is useful to demonstrate all available configuration
	public static List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepParametersProperties jacksonParameters = new SpotlessStepParametersProperties();
		jacksonParameters.putProperty("features",
				ImmutableMap.builder().put(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS.name(), true).build());
		jacksonParameters.putProperty("yaml_features",
				ImmutableMap.builder().put(YAMLGenerator.Feature.MINIMIZE_QUOTES.name(), true).build());
		SpotlessStepProperties jackson =
				SpotlessStepProperties.builder().id("jackson").parameters(jacksonParameters).build();

		return ImmutableList.<SpotlessStepProperties>builder().add(jackson).build();
	}

}
