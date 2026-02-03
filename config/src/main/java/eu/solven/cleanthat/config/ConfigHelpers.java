/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.github.IHasSourceCodeProperties;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.pepper.collection.PepperMapHelper;
import eu.solven.pepper.mappath.MapPathGet;
import lombok.extern.slf4j.Slf4j;

/**
 * Helps working with configuration files
 *
 * @author Benoit Lacelle
 */
@Slf4j
public class ConfigHelpers {

	private static final String KEY_EXCLUDES = "excludes";
	private static final String KEY_INCLUDES = "includes";

	public static final String KEY_SOURCE_CODE = "source_code";

	final Collection<ObjectMapper> objectMappers;
	final ObjectMapper objectMapper;

	public ConfigHelpers(Collection<ObjectMapper> objectMappers) {
		this.objectMappers = objectMappers;
		this.objectMapper = getYaml(objectMappers);
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

	@Deprecated
	public static ObjectMapper makeJsonObjectMapper() {
		return new ObjectMapper();
	}

	public static ObjectMapper makeYamlObjectMapper() {
		var yamlFactory = new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER)
				// This is disabled by default
				.enable(Feature.USE_PLATFORM_LINE_BREAKS);
		var objectMapper = new ObjectMapper(yamlFactory);

		// Used not to print null options in configurations
		// https://www.baeldung.com/jackson-ignore-null-fields
		objectMapper.setSerializationInclusion(Include.NON_NULL);

		return objectMapper;
	}

	public CleanthatRepositoryProperties loadRepoConfig(Resource resource) {
		return loadResource(resource, CleanthatRepositoryProperties.class);
	}

	public CleanthatRepositoryProperties loadResource(Resource resource, Class<CleanthatRepositoryProperties> clazz) {
		ObjectMapper objectMapper;
		var filenameLowerCase = resource.getFilename().toLowerCase(Locale.US);
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

	public IEngineProperties mergeEngineProperties(IHasSourceCodeProperties properties, IEngineProperties dirtyEngine) {
		var dirtyLanguageAsMap = makeDeepCopy(dirtyEngine);
		var sourceConfig = mergeSourceConfig(properties, dirtyLanguageAsMap);
		Map<String, Object> languageConfig = new LinkedHashMap<>();
		languageConfig.putAll(dirtyLanguageAsMap);
		languageConfig.put(KEY_SOURCE_CODE, sourceConfig);
		IEngineProperties languageP = objectMapper.convertValue(languageConfig, CleanthatEngineProperties.class);
		return languageP;
	}

	protected ISourceCodeProperties mergeSourceConfig(IHasSourceCodeProperties properties,
			Map<String, ?> dirtyLanguageConfig) {
		var rootSourceConfigAsMap = objectMapper.convertValue(properties.getSourceCode(), Map.class);
		var explicitSourceCodeProperties =
				MapPathGet.<Map<String, ?>>getOptionalAs(dirtyLanguageConfig, KEY_SOURCE_CODE).orElse(Map.of());

		var sourceConfig = mergeSourceCodeProperties(rootSourceConfigAsMap, explicitSourceCodeProperties);

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
			if (innerLineEnding == null) {
				// We give priority to outer lineEnding in case it is more explicit
				Object outerLineEnding = outer.get("line_ending");
				if (outerLineEnding != null) {
					LOGGER.debug("Outer lineEnding is more explicit than the innerOne");
					merged.put("line_ending", outerLineEnding);
				}
			}

			{
				var outerIncludes = getAsNonNullList(outer, KEY_INCLUDES);
				var innerIncludes = getAsNonNullList(inner, KEY_INCLUDES);

				var outerExcludes = getAsNonNullList(outer, KEY_EXCLUDES);
				var innerExcludes = getAsNonNullList(inner, KEY_EXCLUDES);

				if (innerIncludes.isEmpty()) {
					// An inner excludes cancels outer includes
					var outerIncludesWithoutInnerExclude =
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
				var outerExcludesWithoutInnerInclude =
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
		var outerIncludes = (List<?>) outer.get(k);
		if (outerIncludes == null) {
			outerIncludes = Collections.emptyList();
		}
		return outerIncludes;
	}

	public <T> Map<String, Object> makeDeepCopy(T object) {
		try {
			// We make a deep-copy before mutation
			var serialized = objectMapper.writeValueAsBytes(object);
			var fromJackson = objectMapper.readValue(serialized, Map.class);

			return new LinkedHashMap<>(fromJackson);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Issue with: " + object, e);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue with: " + object, e);
		}
	}

	public IEngineProperties forceIncludes(IEngineProperties engine, Collection<String> includes) {
		var engineAsMap = objectMapper.convertValue(engine, Map.class);
		var sourceCodeAsMap = objectMapper.convertValue(engine.getSourceCode(), Map.class);
		sourceCodeAsMap.put(KEY_INCLUDES, includes);
		engineAsMap.put(KEY_SOURCE_CODE, sourceCodeAsMap);
		return objectMapper.convertValue(engineAsMap, CleanthatEngineProperties.class);
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
