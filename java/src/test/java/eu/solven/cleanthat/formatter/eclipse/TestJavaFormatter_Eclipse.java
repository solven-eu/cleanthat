package eu.solven.cleanthat.formatter.eclipse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import io.cleanthat.do_not_format_me.CleanClass;
import io.cleanthat.do_not_format_me.ManySpacesBetweenImportsSimpleClass;

public class TestJavaFormatter_Eclipse {

	final ObjectMapper objectMapper = new ObjectMapper();

	// https://www.baeldung.com/spring-load-resource-as-string
	public static String asString(Resource resource) {
		try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			return FileCopyUtils.copyToString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	final JavaFormatter formatter = new JavaFormatter(objectMapper);

	private ILanguageProperties getLanguageProperties() throws IOException, JsonParseException, JsonMappingException {
		CleanthatRepositoryProperties properties = new ConfigHelpers(objectMapper)
				.loadRepoConfig(new ClassPathResource("/config/" + "eclipseformatter_cleanthat.json"));

		List<Map<String, ?>> languages = properties.getLanguages();
		Assert.assertEquals(1, languages.size());
		ILanguageProperties languageP =
				new ConfigHelpers(objectMapper).mergeLanguageProperties(properties, languages.get(0));
		return languageP;
	}

	@Test
	public void testFormat_Clean() throws IOException {
		ILanguageProperties languageP = getLanguageProperties();

		URL location = CleanClass.class.getProtectionDomain().getCodeSource().getLocation();
		String classAsString = asString(new UrlResource(location));

		String cleaned = formatter.format(languageP, "someFilePath", classAsString);
		Assert.assertEquals(cleaned, classAsString);

		Assert.assertEquals(1, formatter.getCacheSize());
	}

	@Test
	public void testFormat_WrongIndentation() throws IOException {
		ILanguageProperties languageP = getLanguageProperties();

		URL location = ManySpacesBetweenImportsSimpleClass.class.getProtectionDomain().getCodeSource().getLocation();
		String classAsString = asString(new UrlResource(location));

		String cleaned = formatter.format(languageP, "someFilePath", classAsString);
		Assert.assertEquals(cleaned, classAsString);

		Assert.assertEquals(1, formatter.getCacheSize());
	}

	@Test
	public void testFormat_WrongIndentation_Multiple() throws IOException {
		ILanguageProperties languageP = getLanguageProperties();

		URL location = ManySpacesBetweenImportsSimpleClass.class.getProtectionDomain().getCodeSource().getLocation();
		String classAsString = asString(new UrlResource(location));

		// Format twice
		formatter.format(languageP, "someFilePath", classAsString);
		formatter.format(languageP, "someFilePath", classAsString);

		// Check the cache is used properly
		Assert.assertEquals(1, formatter.getCacheSize());
	}
}
