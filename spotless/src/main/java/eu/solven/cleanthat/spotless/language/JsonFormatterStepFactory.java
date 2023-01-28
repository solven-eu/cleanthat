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
package eu.solven.cleanthat.spotless.language;

import java.util.List;
import java.util.Map;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.json.JacksonJsonConfig;
import com.diffplug.spotless.json.JacksonJsonStep;
import com.diffplug.spotless.json.JsonSimpleStep;
import com.diffplug.spotless.json.gson.GsonConfig;
import com.diffplug.spotless.json.gson.GsonStep;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;

/**
 * Configure Spotless engine for JSON files
 * 
 * @author Benoit Lacelle
 *
 */
public class JsonFormatterStepFactory extends AFormatterStepFactory {
	private static final int DEFAULT_INDENTATION = 4;
	private static final String DEFAULT_GSON_VERSION = "2.10.1";

	public JsonFormatterStepFactory(JsonFormatterFactory jsonFactory,
			ICodeProvider codeProvider,
			SpotlessFormatterProperties spotlessProperties) {
		super(jsonFactory, codeProvider, spotlessProperties);
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
		case "simple": {
			return makeSimple(s, provisioner);
		}
		case "gson": {
			return makeGson(s, provisioner);
		}
		case "jackson": {
			return makeJackson(s, provisioner);
		}
		default: {
			throw new IllegalArgumentException("Unknown step: " + stepId);
		}
		}
	}

	private FormatterStep makeSimple(SpotlessStepProperties s, Provisioner provisioner) {
		Integer indentSpaces = s.getCustomProperty("indent_spaces", Integer.class);
		if (indentSpaces == null) {
			indentSpaces = DEFAULT_INDENTATION;
		}

		return JsonSimpleStep.create(indentSpaces, provisioner);
	}

	private FormatterStep makeGson(SpotlessStepProperties s, Provisioner provisioner) {
		Integer indentSpaces = s.getCustomProperty("indent_spaces", Integer.class);
		if (indentSpaces == null) {
			indentSpaces = DEFAULT_INDENTATION;
		}

		Boolean sortByKeys = s.getCustomProperty("sort_by_keys", Boolean.class);
		if (sortByKeys == null) {
			sortByKeys = false;
		}

		Boolean escapeHtml = s.getCustomProperty("escape_html", Boolean.class);
		if (escapeHtml == null) {
			escapeHtml = false;
		}

		String version = s.getCustomProperty("version", String.class);
		if (version == null) {
			version = DEFAULT_GSON_VERSION;
		}

		return GsonStep.create(new GsonConfig(sortByKeys, escapeHtml, indentSpaces, version), provisioner);
	}

	private FormatterStep makeJackson(SpotlessStepProperties s, Provisioner provisioner) {
		JacksonJsonConfig jacksonConfig = new JacksonJsonConfig();

		Boolean spaceBeforeSeparator = s.getCustomProperty("space_before_separator", Boolean.class);
		if (spaceBeforeSeparator != null) {
			jacksonConfig.setSpaceBeforeSeparator(spaceBeforeSeparator);
		}

		@SuppressWarnings("unchecked")
		Map<String, Boolean> features = s.getCustomProperty("features", Map.class);
		if (features != null) {
			jacksonConfig.appendFeatureToToggle(features);
		}

		@SuppressWarnings("unchecked")
		Map<String, Boolean> jsonFeatures = s.getCustomProperty("json_features", Map.class);
		if (features != null) {
			jacksonConfig.appendJsonFeatureToToggle(jsonFeatures);
		}

		String version = s.getCustomProperty("version", String.class);
		if (version == null) {
			version = JacksonJsonStep.defaultVersion();
		}

		return JacksonJsonStep.create(jacksonConfig, version, provisioner);
	}

	// This is useful to demonstrate all available configuration
	public static List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties jackson = new SpotlessStepProperties();
		jackson.setId("jackson");
		jackson.putProperty("features",
				ImmutableMap.builder().put(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS.name(), true).build());
		jackson.putProperty("yaml_features",
				ImmutableMap.builder().put(JsonGenerator.Feature.QUOTE_FIELD_NAMES.name(), false).build());

		return ImmutableList.<SpotlessStepProperties>builder().add(jackson).build();
	}

}
