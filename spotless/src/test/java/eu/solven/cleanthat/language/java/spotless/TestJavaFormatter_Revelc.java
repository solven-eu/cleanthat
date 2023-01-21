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
package eu.solven.cleanthat.language.java.spotless;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.do_not_format_me.CleanClass;
import eu.solven.cleanthat.do_not_format_me.ManySpacesBetweenImportsSimpleClass;
import eu.solven.cleanthat.engine.ICodeFormatterApplier;
import eu.solven.cleanthat.formatter.CodeFormatterApplier;
import eu.solven.cleanthat.formatter.SourceCodeFormatterHelper;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.cleanthat.language.spotless.SpotlessFormattersFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.FileCopyUtils;

public class TestJavaFormatter_Revelc {

	final ObjectMapper objectMapper = new ObjectMapper();
	final ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(objectMapper));

	// https://www.baeldung.com/spring-load-resource-as-string
	public static String asString(Resource resource) {
		try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			return FileCopyUtils.copyToString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	final SpotlessFormattersFactory formatter = new SpotlessFormattersFactory(configHelpers);
	final ICodeFormatterApplier applier = new CodeFormatterApplier();
	final SourceCodeFormatterHelper helper = new SourceCodeFormatterHelper(objectMapper);

	private IEngineProperties getLanguageProperties() throws IOException, JsonParseException, JsonMappingException {
		CleanthatRepositoryProperties properties =
				configHelpers.loadRepoConfig(new ClassPathResource("/config/" + "revelcimport_cleanthat.json"));

		List<CleanthatEngineProperties> languages = properties.getEngines();
		Assert.assertEquals(1, languages.size());
		IEngineProperties languageP =
				new ConfigHelpers(Arrays.asList(objectMapper)).mergeEngineProperties(properties, languages.get(0));
		return languageP;
	}

	@Test
	public void testFormat_Clean() throws IOException {
		IEngineProperties languageP = getLanguageProperties();

		URL location = CleanClass.class.getProtectionDomain().getCodeSource().getLocation();
		String classAsString = asString(new UrlResource(location));

		String cleaned =
				applier.applyProcessors(helper.compile(languageP, null, formatter), "someFilePath", classAsString);
		Assert.assertEquals(cleaned, classAsString);

		// Assert.assertEquals(0, formatter.getCacheSize());
	}

	@Test
	public void testFormat_ManySpacesMiddleImports() throws IOException {
		IEngineProperties languageP = getLanguageProperties();

		URL location = ManySpacesBetweenImportsSimpleClass.class.getProtectionDomain().getCodeSource().getLocation();
		String classAsString = asString(new UrlResource(location));

		String cleaned =
				applier.applyProcessors(helper.compile(languageP, null, formatter), "someFilePath", classAsString);
		Assert.assertEquals(cleaned, classAsString);

		// Assert.assertEquals(0, formatter.getCacheSize());
	}
}
