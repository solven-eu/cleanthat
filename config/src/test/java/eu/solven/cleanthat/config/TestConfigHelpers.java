package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.formatter.LineEnding;

public class TestConfigHelpers {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestConfigHelpers.class);

	private static final String EOL = System.lineSeparator();

	@Test
	public void testYaml() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();
		String asString = yamlObjectMapper.writeValueAsString(Map.of("k1", Map.of("k2", "v")));
		// This may demonstrate unexpected behavior with EOL on different systems
		Assertions.assertThat(asString).contains("k", "  k2: \"v\"");
		Assertions.assertThat(asString.split(EOL)).hasSize(2);
	}

	@Test
	public void testFromJsonToYaml() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper jsonObjectMapper = ConfigHelpers.makeJsonObjectMapper();
		ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(jsonObjectMapper));
		// 'default_as_json' case is not satisfying as we have null in its yaml version
		Stream.of("simple_as_json", "default_as_json").forEach(name -> {
			try {
				CleanthatRepositoryProperties config =
						configHelpers.loadRepoConfig(new ClassPathResource("/config/" + name + ".json"));
				ObjectMapper yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();
				String asYaml = yamlObjectMapper.writeValueAsString(config);
				LOGGER.debug("Config as yaml: {}{}{}{}{}{}", EOL, "------", EOL, asYaml, EOL, "------");
				String expectedYaml = StreamUtils.copyToString(
						new ClassPathResource("/config/" + name + ".to_yaml.yaml").getInputStream(),
						StandardCharsets.UTF_8);
				if ("\r\n".equals(EOL)) {
					// We are seemingly under Windows
					if (!expectedYaml.contains(EOL)) {
						Assert.fail("Files are not checked-out with system EOL");
					} else if (!asYaml.contains(EOL)) {
						Assert.fail("YAML are not generated with system EOL");
					}
				}
				Assert.assertEquals("Issue with " + name, expectedYaml, asYaml);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue with: " + name, e);
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMergeSourceCodeEol() {
		ObjectMapper om = ConfigHelpers.makeJsonObjectMapper();
		ConfigHelpers helper = new ConfigHelpers(Collections.singleton(om));

		SourceCodeProperties defaultP = new SourceCodeProperties();
		defaultP.setEncoding(StandardCharsets.ISO_8859_1.name());
		SourceCodeProperties windowsP = new SourceCodeProperties();
		windowsP.setLineEndingAsEnum(LineEnding.CRLF);
		windowsP.setEncoding(StandardCharsets.US_ASCII.name());

		Assert.assertEquals(LineEnding.UNKNOWN, defaultP.getLineEndingAsEnum());
		Assert.assertEquals(LineEnding.CRLF, windowsP.getLineEndingAsEnum());

		Assert.assertEquals("ISO-8859-1", defaultP.getEncoding());
		Assert.assertEquals("US-ASCII", windowsP.getEncoding());

		// windows as inner
		{
			Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(defaultP, Map.class),
					om.convertValue(windowsP, Map.class));
			SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

			Assert.assertEquals(LineEnding.CRLF, merged.getLineEndingAsEnum());
			Assert.assertEquals("US-ASCII", merged.getEncoding());
		}

		// windows as outer
		{
			Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(windowsP, Map.class),
					om.convertValue(defaultP, Map.class));
			SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

			Assert.assertEquals(LineEnding.CRLF, merged.getLineEndingAsEnum());
			Assert.assertEquals("ISO-8859-1", merged.getEncoding());
		}

		// default and default
		{
			Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(defaultP, Map.class),
					om.convertValue(defaultP, Map.class));
			SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

			Assert.assertEquals(LineEnding.UNKNOWN, merged.getLineEndingAsEnum());
			Assert.assertEquals("ISO-8859-1", merged.getEncoding());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMergeSourceCode_parentHasExcluded() {
		ObjectMapper om = ConfigHelpers.makeJsonObjectMapper();
		ConfigHelpers helper = new ConfigHelpers(Collections.singleton(om));

		SourceCodeProperties defaultP = new SourceCodeProperties();
		defaultP.setExcludes(Arrays.asList(".*/generated/.*"));

		{
			// EmptyChildren
			SourceCodeProperties childrenP = new SourceCodeProperties();

			Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(defaultP, Map.class),
					om.convertValue(childrenP, Map.class));
			SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

			Assert.assertEquals(Arrays.asList(".*/generated/.*"), merged.getExcludes());
		}

		{
			// NotEmptyChildren
			SourceCodeProperties childrenP = new SourceCodeProperties();
			childrenP.setExcludes(Arrays.asList(".*/do_not_clean_me/.*"));

			Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(defaultP, Map.class),
					om.convertValue(childrenP, Map.class));
			SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

			Assert.assertEquals(Arrays.asList(".*/generated/.*", ".*/do_not_clean_me/.*"), merged.getExcludes());
		}

		{
			// NotEmptyChildren and cancel parent exclusion
			SourceCodeProperties childrenP = new SourceCodeProperties();
			childrenP.setExcludes(Arrays.asList(".*/do_not_clean_me/.*"));
			childrenP.setIncludes(Arrays.asList(".*/generated/.*"));

			Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(defaultP, Map.class),
					om.convertValue(childrenP, Map.class));
			SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

			Assert.assertEquals(Arrays.asList(".*/do_not_clean_me/.*"), merged.getExcludes());
			Assert.assertEquals(Arrays.asList(".*/generated/.*"), merged.getIncludes());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMergeSourceCode_parentHasIncluded() {
		ObjectMapper om = ConfigHelpers.makeJsonObjectMapper();
		ConfigHelpers helper = new ConfigHelpers(Collections.singleton(om));

		SourceCodeProperties defaultP = new SourceCodeProperties();
		defaultP.setIncludes(Arrays.asList(".*\\.xml"));

		{
			// EmptyChildren
			SourceCodeProperties childrenP = new SourceCodeProperties();

			Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(defaultP, Map.class),
					om.convertValue(childrenP, Map.class));
			SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

			Assert.assertEquals(Arrays.asList(".*\\.xml"), merged.getIncludes());
		}

		{
			// NotEmptyChildren
			SourceCodeProperties childrenP = new SourceCodeProperties();
			childrenP.setIncludes(Arrays.asList("pom.xml"));

			Map<String, ?> mergedAsMap = helper.mergeSourceCodeProperties(om.convertValue(defaultP, Map.class),
					om.convertValue(childrenP, Map.class));
			SourceCodeProperties merged = om.convertValue(mergedAsMap, SourceCodeProperties.class);

			Assert.assertEquals(Arrays.asList("pom.xml"), merged.getIncludes());
		}
	}
}
