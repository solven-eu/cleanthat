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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.Iterables;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.config.pojo.EngineProperties;
import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.github.IHasSourceCodeProperties;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.pepper.collection.PepperMapHelper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Helps working with configuration files
 *
 * @author Benoit Lacelle
 */
public class ConfigHelpers {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigHelpers.class);

	private static final String KEY_EXCLUDES = "excludes";
	private static final String KEY_INCLUDES = "includes";

	public static final String KEY_SOURCE_CODE = "source_code";

	final Collection<ObjectMapper> objectMappers;
	final ObjectMapper objectMapper;

	public ConfigHelpers(Collection<ObjectMapper> objectMappers) {
		this.objectMappers = objectMappers;
		this.objectMapper = Iterables.get(objectMappers, 0);
	}

	public static ConfigHelpers forTests() {
		return new ConfigHelpers(Arrays.asList(ConfigHelpers.makeYamlObjectMapper()));
	}

	/**
	 * 
	 * @return some default {@link ObjectMapper}. May be specialized for JSON, or YAML.
	 */
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public static ObjectMapper makeJsonObjectMapper() {
		return new ObjectMapper();
	}

	public static ObjectMapper makeYamlObjectMapper() {
		YAMLFactory yamlFactory = new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER)
				// This is disabled by default
				.enable(Feature.USE_PLATFORM_LINE_BREAKS);
		ObjectMapper objectMapper = new ObjectMapper(yamlFactory);
		return objectMapper;
	}

	public CleanthatRepositoryProperties loadRepoConfig(Resource resource) {
		return loadResource(resource, CleanthatRepositoryProperties.class);
	}

	public CleanthatRepositoryProperties loadResource(Resource resource, Class<CleanthatRepositoryProperties> clazz) {
		ObjectMapper objectMapper;
		String filenameLowerCase = resource.getFilename().toLowerCase(Locale.US);
		if (filenameLowerCase.endsWith(".json")) {
			objectMapper = ConfigHelpers.getJson(objectMappers);
		} else if (filenameLowerCase.endsWith(".yml") || filenameLowerCase.endsWith(".yaml")) {
			objectMapper = ConfigHelpers.getYaml(objectMappers);
		} else {
			throw new IllegalArgumentException("Not managed filename: " + filenameLowerCase);
		}
		try {
			return objectMapper.readValue(resource.getInputStream(), clazz);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue loading: " + resource, e);
		} catch (Exception e) {
			throw new IllegalArgumentException("Issue loading: " + resource, e);
		}
	}

	public IEngineProperties mergeLanguageProperties(IHasSourceCodeProperties properties,
			IEngineProperties dirtyLanguageConfig) {
		Map<String, ?> dirtyLanguageAsMap = makeDeepCopy(dirtyLanguageConfig);
		ISourceCodeProperties sourceConfig = mergeSourceConfig(properties, dirtyLanguageAsMap);
		Map<String, Object> languageConfig = new LinkedHashMap<>();
		languageConfig.putAll(dirtyLanguageAsMap);
		languageConfig.put(KEY_SOURCE_CODE, sourceConfig);
		IEngineProperties languageP = objectMapper.convertValue(languageConfig, EngineProperties.class);
		return languageP;
	}

	// Duplicates eu.solven.cleanthat.config.ConfigHelpers.mergeLanguageProperties(CleanthatRepositoryProperties,
	// Map<String, ?>) ?
	public IEngineProperties mergeLanguageIntoProcessorProperties(IEngineProperties languagePropertiesTemplate,
			Map<String, ?> rawProcessor) {
		Map<String, Object> languagePropertiesAsMap = makeDeepCopy(languagePropertiesTemplate);
		// As we are processing a single processor, we can get ride of the processors field
		languagePropertiesAsMap.remove("processors");
		// A processor may need to be applied with an overriden languageVersion
		// Optional<String> optLanguageVersionOverload =
		// PepperMapHelper.getOptionalString(rawProcessor, "language_version");
		// if (optLanguageVersionOverload.isPresent()) {
		// languagePropertiesAsMap.put("language_version", optLanguageVersionOverload.get());
		// }
		Optional<Map<String, ?>> optSourceOverloads = PepperMapHelper.getOptionalAs(rawProcessor, KEY_SOURCE_CODE);
		if (optSourceOverloads.isPresent()) {
			// Mutable copy
			Map<String, Object> sourcePropertiesAsMap =
					mergeSourceCodeProperties(PepperMapHelper.getRequiredMap(languagePropertiesAsMap, KEY_SOURCE_CODE),
							optSourceOverloads.get());

			// Re-inject
			languagePropertiesAsMap.put(KEY_SOURCE_CODE, sourcePropertiesAsMap);
		}
		IEngineProperties languageProperties =
				objectMapper.convertValue(languagePropertiesAsMap, EngineProperties.class);
		return languageProperties;
	}

	protected ISourceCodeProperties mergeSourceConfig(IHasSourceCodeProperties properties,
			Map<String, ?> dirtyLanguageConfig) {
		Map<String, ?> rootSourceConfigAsMap = objectMapper.convertValue(properties.getSourceCode(), Map.class);
		Map<String, ?> explicitSourceCodeProperties =
				PepperMapHelper.getRequiredMap(dirtyLanguageConfig, KEY_SOURCE_CODE);

		Map<String, Object> sourceConfig =
				mergeSourceCodeProperties(rootSourceConfigAsMap, explicitSourceCodeProperties);

		return objectMapper.convertValue(sourceConfig, SourceCodeProperties.class);
	}

	protected Map<String, Object> mergeSourceCodeProperties(Map<String, ?> outer, Map<String, ?> inner) {
		Map<String, Object> merged = new LinkedHashMap<>();

		if (outer != null) {
			merged.putAll(outer);
		}

		if (inner != null) {
			// Inner has priority over outer
			merged.putAll(inner);
		}

		if (outer != null && inner != null) {
			Object innerLineEnding = inner.get("line_ending");
			if (innerLineEnding == null
					|| Set.of(LineEnding.UNKNOWN, LineEnding.UNKNOWN.toString()).contains(innerLineEnding)) {
				// We give priority to outer lineEnding in case it is more explicit
				Object outerLineEnding = outer.get("line_ending");
				if (outerLineEnding != null) {
					LOGGER.debug("Outer lineEnding is more explicit than the innerOne");
					merged.put("line_ending", outerLineEnding);
				}
			}

			{
				List<?> outerIncludes = getAsNonNullList(outer, KEY_INCLUDES);
				List<?> innerIncludes = getAsNonNullList(inner, KEY_INCLUDES);

				List<?> outerExcludes = getAsNonNullList(outer, KEY_EXCLUDES);
				List<?> innerExcludes = getAsNonNullList(inner, KEY_EXCLUDES);

				if (innerIncludes.isEmpty()) {
					// An inner excludes cancels outer includes
					Stream<?> outerIncludesWithoutInnerExclude =
							outerIncludes.stream().filter(includes -> !innerExcludes.contains(includes));

					List<Object> mergedIncludes =
							Stream.concat(outerIncludesWithoutInnerExclude, innerIncludes.stream())
									.distinct()
									.collect(Collectors.toList());

					merged.put(KEY_INCLUDES, mergedIncludes);
				} else {
					// We discard outer includes to rely only on inner includes
					merged.put(KEY_INCLUDES, innerIncludes);
				}

				// An inner includes cancels outer excludes
				Stream<?> outerExcludesWithoutInnerInclude =
						outerExcludes.stream().filter(exclude -> !innerIncludes.contains(exclude));
				List<Object> mergedExcludes = Stream.concat(outerExcludesWithoutInnerInclude, innerExcludes.stream())
						.distinct()
						.collect(Collectors.toList());

				merged.put(KEY_EXCLUDES, mergedExcludes);
			}
		}

		return merged;
	}

	private List<?> getAsNonNullList(Map<String, ?> outer, String k) {
		List<?> outerIncludes = (List<?>) outer.get(k);
		if (outerIncludes == null) {
			outerIncludes = Collections.emptyList();
		}
		return outerIncludes;
	}

	public <T> Map<String, Object> makeDeepCopy(T object) {
		try {
			// We make a deep-copy before mutation
			byte[] serialized = objectMapper.writeValueAsBytes(object);
			Map<String, ?> fromJackson = objectMapper.readValue(serialized, Map.class);

			return new LinkedHashMap<>(fromJackson);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Issue with: " + object, e);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue with: " + object, e);
		}
	}

	public IEngineProperties forceIncludes(IEngineProperties languageP, List<String> includes) {
		Map<String, Object> languageAsMap = objectMapper.convertValue(languageP, Map.class);
		Map<String, Object> sourceCodeAsMap = objectMapper.convertValue(languageP.getSourceCode(), Map.class);
		sourceCodeAsMap.put(KEY_INCLUDES, includes);
		languageAsMap.put(KEY_SOURCE_CODE, sourceCodeAsMap);
		return objectMapper.convertValue(languageAsMap, EngineProperties.class);
	}

	public static ObjectMapper getJson(Collection<ObjectMapper> objectMappers) {
		return objectMappers.stream()
				.filter(om -> JsonFactory.FORMAT_NAME_JSON.equals(om.getFactory().getFormatName()))
				.findAny()
				.get();
	}

	public static ObjectMapper getYaml(Collection<ObjectMapper> objectMappers) {
		return objectMappers.stream()
				.filter(om -> YAMLFactory.FORMAT_NAME_YAML.equals(om.getFactory().getFormatName()))
				.findAny()
				.get();
	}
}
