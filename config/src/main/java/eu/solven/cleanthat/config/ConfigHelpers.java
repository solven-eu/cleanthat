package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.github.CleanthatLanguageProperties;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.github.ISourceCodeProperties;
import eu.solven.cleanthat.github.SourceCodeProperties;

/**
 * Helps working with configuration files
 *
 * @author Benoit Lacelle
 */
public class ConfigHelpers {

	final List<ObjectMapper> objectMappers;

	public ConfigHelpers(List<ObjectMapper> objectMappers) {
		this.objectMappers = objectMappers;
	}

	public static ObjectMapper makeJsonObjectMapper() {
		return new ObjectMapper();
	}

	public static ObjectMapper makeYamlObjectMapper() {
		return new ObjectMapper(new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER));
	}

	public CleanthatRepositoryProperties loadRepoConfig(Resource resource) {
		ObjectMapper objectMapper;
		if (resource.getFilename().endsWith("json")) {
			objectMapper = ConfigHelpers.getJson(objectMappers);
		} else {
			objectMapper = ConfigHelpers.getYaml(objectMappers);
		}
		try {
			return objectMapper.readValue(resource.getInputStream(), CleanthatRepositoryProperties.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue loading: " + resource, e);
		} catch (Exception e) {
			throw new IllegalArgumentException("Issue loading: " + resource, e);
		}
	}

	protected ISourceCodeProperties mergeSourceConfig(CleanthatRepositoryProperties properties,
			Map<String, ?> dirtyLanguageConfig) {
		Map<String, Object> sourceConfig = new LinkedHashMap<>();
		// Apply defaults from parent
		sourceConfig.putAll(objectMappers.get(0).convertValue(properties.getSourceCodeProperties(), Map.class));
		// Apply explicit configuration
		Map<String, ?> explicitSourceCodeProperties = PepperMapHelper.getAs(dirtyLanguageConfig, "source_code");
		if (explicitSourceCodeProperties != null) {
			sourceConfig.putAll(explicitSourceCodeProperties);
		}
		return objectMappers.get(0).convertValue(sourceConfig, SourceCodeProperties.class);
	}

	public ILanguageProperties mergeLanguageProperties(CleanthatRepositoryProperties properties,
			Map<String, ?> dirtyLanguageConfig) {
		ISourceCodeProperties sourceConfig = mergeSourceConfig(properties, dirtyLanguageConfig);
		Map<String, Object> languageConfig = new LinkedHashMap<>();
		languageConfig.putAll(dirtyLanguageConfig);
		languageConfig.put("source_code", sourceConfig);
		ILanguageProperties languageP =
				objectMappers.get(0).convertValue(languageConfig, CleanthatLanguageProperties.class);
		return languageP;
	}

	public ILanguageProperties forceIncludes(ILanguageProperties languageP, List<String> includes) {
		Map<String, Object> languageAsMap = objectMappers.get(0).convertValue(languageP, Map.class);
		Map<String, Object> sourceCodeAsMap =
				objectMappers.get(0).convertValue(languageP.getSourceCodeProperties(), Map.class);
		sourceCodeAsMap.put("includes", includes);
		languageAsMap.put("source_code", sourceCodeAsMap);
		return objectMappers.get(0).convertValue(languageAsMap, CleanthatLanguageProperties.class);
	}

	public static ObjectMapper getJson(List<ObjectMapper> objectMappers) {
		return objectMappers.stream()
				.filter(om -> JsonFactory.FORMAT_NAME_JSON.equals(om.getFactory().getFormatName()))
				.findAny()
				.get();
	}

	public static ObjectMapper getYaml(List<ObjectMapper> objectMappers) {
		return objectMappers.stream()
				.filter(om -> !JsonFactory.FORMAT_NAME_JSON.equals(om.getFactory().getFormatName()))
				.findAny()
				.get();
	}
}
