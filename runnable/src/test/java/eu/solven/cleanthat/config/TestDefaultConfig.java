package eu.solven.cleanthat.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.java.mutators.JavaRulesMutatorProperties;
import eu.solven.cleanthat.language.CleanthatLanguageProperties;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.java.imports.JavaRevelcImportsCleanerProperties;
import eu.solven.cleanthat.language.java.spring.SpringJavaFormatterProperties;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class TestDefaultConfig {
	// private static final Logger LOGGER = LoggerFactory.getLogger(TestDefaultConfig.class);

	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(CleanthatRepositoryProperties.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testFromJsonToYaml() throws JsonParseException, JsonMappingException, IOException {
		// ObjectMapper jsonObjectMapper = ConfigHelpers.makeJsonObjectMapper();
		ObjectMapper yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();

		// 'default_as_json' case is not satisfying as we have null in its yaml version
		// try {

		CleanthatRepositoryProperties configFromEmpty =
				yamlObjectMapper.convertValue(Map.of(), CleanthatRepositoryProperties.class);

		// By in safe-default, we exclude anything in an 'exclude' directory
		{
			Assertions.assertThat(configFromEmpty.getSourceCode().getExcludes()).isEmpty();
			configFromEmpty.getSourceCode().setExcludes(Arrays.asList("regex:.*/generated/.*"));
		}

		{

			Assertions.assertThat(configFromEmpty.getLanguages()).isEmpty();
			// Ensure mutability
			configFromEmpty.setLanguages(new ArrayList<>());

			CleanthatLanguageProperties javaProperties = new CleanthatLanguageProperties();
			{
				javaProperties.setLanguage("java");
				javaProperties.setLanguageVersion("11");
				SourceCodeProperties javaSourceCodeProperties = new SourceCodeProperties();
				javaSourceCodeProperties.setIncludes(Arrays.asList("regex:.*\\.java"));
				javaProperties.setSourceCode(javaSourceCodeProperties);

				Assertions.assertThat(javaProperties.getProcessors()).isEmpty();
				javaProperties.setProcessors(new ArrayList<>());

				javaProperties.getProcessors()
						.add(ImmutableMap.<String, Object>builder()
								.put("engine", "rules")
								.put("parameters", JavaRulesMutatorProperties.defaults())
								.build());

				javaProperties.getProcessors()
						.add(ImmutableMap.<String, Object>builder()
								.put("engine", "revelc_imports")
								.put("parameters", new JavaRevelcImportsCleanerProperties())
								.build());

				javaProperties.getProcessors()
						.add(ImmutableMap.<String, Object>builder()
								.put("engine", "spring_formatter")
								.put("parameters", new SpringJavaFormatterProperties())
								.build());
				//
				// javaProperties.getProcessors().forEach(processor -> {
				// Object parameters = PepperMapHelper.getRequiredAs(processor, "parameters");
				//
				// processor.set
				// });
			}

			configFromEmpty.getLanguages().add(javaProperties);
		}

		// This is useful to convert the Java class of processors into Map (like it will happen when loading from the
		// yaml)
		CleanthatRepositoryProperties configFromEmptyAsMap = yamlObjectMapper
				.readValue(yamlObjectMapper.writeValueAsString(configFromEmpty), CleanthatRepositoryProperties.class);

		ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(yamlObjectMapper));
		CleanthatRepositoryProperties configDefaultSafe =
				configHelpers.loadRepoConfig(new ClassPathResource("/config/default-safe.yaml"));

		Assert.assertEquals(configDefaultSafe.toString(), configFromEmptyAsMap.toString());
		Assert.assertEquals(configDefaultSafe, configFromEmptyAsMap);
	}
}
