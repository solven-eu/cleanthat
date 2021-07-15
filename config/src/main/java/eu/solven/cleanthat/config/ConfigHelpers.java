package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

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
 *
 */
public class ConfigHelpers {

	final ObjectMapper objectMapper;

	public ConfigHelpers(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public CleanthatRepositoryProperties loadRepoConfig(Resource resource) {
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
		sourceConfig.putAll(objectMapper.convertValue(properties.getSourceCodeProperties(), Map.class));
		// Apply explicit configuration
		Map<String, ?> explicitSourceCodeProperties = PepperMapHelper.getAs(dirtyLanguageConfig, "source_code");
		if (explicitSourceCodeProperties != null) {
			sourceConfig.putAll(explicitSourceCodeProperties);
		}
		return objectMapper.convertValue(sourceConfig, SourceCodeProperties.class);
	}

	public ILanguageProperties mergeLanguageProperties(CleanthatRepositoryProperties properties,
			Map<String, ?> dirtyLanguageConfig) {
		ISourceCodeProperties sourceConfig = mergeSourceConfig(properties, dirtyLanguageConfig);
		Map<String, Object> languageConfig = new LinkedHashMap<>();
		languageConfig.putAll(dirtyLanguageConfig);
		languageConfig.put("source_code", sourceConfig);
		ILanguageProperties languageP = objectMapper.convertValue(languageConfig, CleanthatLanguageProperties.class);
		return languageP;
	}

	public ILanguageProperties forceIncludes(ILanguageProperties languageP, List<String> includes) {
		Map<String, Object> languageAsMap = objectMapper.convertValue(languageP, Map.class);

		Map<String, Object> sourceCodeAsMap = objectMapper.convertValue(languageP.getSourceCodeProperties(), Map.class);
		sourceCodeAsMap.put("includes", includes);

		languageAsMap.put("source_code", sourceCodeAsMap);

		return objectMapper.convertValue(languageAsMap, CleanthatLanguageProperties.class);
	}
}
