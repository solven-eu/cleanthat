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
package eu.solven.cleanthat.language.java.spotless;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Jimfs;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.engine.EngineAndLinters;
import eu.solven.cleanthat.engine.ICodeFormatterApplier;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.formatter.CodeFormatterApplier;
import eu.solven.cleanthat.formatter.PathAndContent;
import eu.solven.cleanthat.formatter.SourceCodeFormatterHelper;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.cleanthat.language.spotless.SpotlessFormattersFactory;
import eu.solven.pepper.resource.PepperResourceHelper;

public class TestSpotlessFormatter_Eclipse {

	final ObjectMapper objectMapper = ConfigHelpers.makeYamlObjectMapper();

	// https://www.baeldung.com/spring-load-resource-as-string
	public static String asString(Resource resource) {
		try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			return FileCopyUtils.copyToString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	final SpotlessFormattersFactory formatter =
			new SpotlessFormattersFactory(new ConfigHelpers(Arrays.asList(objectMapper)),
					SpotlessFormattersFactory.makeProvisioner());
	final ICodeFormatterApplier applier = new CodeFormatterApplier();
	final SourceCodeFormatterHelper helper = new SourceCodeFormatterHelper();

	final CleanthatRepositoryProperties repositoryProperties = new ConfigHelpers(Arrays.asList(objectMapper))
			.loadRepoConfig(new ClassPathResource("/config/" + "cleanthat_requestSpotless_requestEclipse.yaml"));

	private IEngineProperties getEngineProperties() throws IOException, JsonParseException, JsonMappingException {
		List<CleanthatEngineProperties> engines = repositoryProperties.getEngines();
		Assert.assertEquals(1, engines.size());
		var engineP = new ConfigHelpers(Arrays.asList(objectMapper)).mergeEngineProperties(repositoryProperties,
				engines.get(0));
		return engineP;
	}

	final FileSystem fileSystem;
	final ICodeProvider classpathCodeProvider = new ICodeProvider() {

		@Override
		public Optional<String> loadContentForPath(Path path) throws IOException {
			return Optional.of(PepperResourceHelper.loadAsString(path.toString()));
		}

		@Override
		public void listFilesForContent(Set<String> includePatterns, Consumer<ICodeProviderFile> consumer)
				throws IOException {
			throw new IllegalArgumentException("Not implemented");
		}

		@Override
		public String getRepoUri() {
			return Thread.currentThread().getContextClassLoader().getName();
		}

		@Override
		public Path getRepositoryRoot() {
			return fileSystem.getPath(fileSystem.getSeparator());
		}
	};

	CleanthatSession cleanthatSession;
	{
		fileSystem = Jimfs.newFileSystem();
		cleanthatSession = new CleanthatSession(fileSystem.getPath(fileSystem.getSeparator()),
				classpathCodeProvider,
				repositoryProperties);
	}

	@Before
	public void resetCounters() {
		CodeFormatterApplier.NB_EXCEPTIONS.set(0);
	}

	@After
	public void checkCounters() {
		Assertions.assertThat(CodeFormatterApplier.NB_EXCEPTIONS.get()).isEqualTo(0);
	}

	@Test
	public void testFormat_WrongIndentation() throws IOException {
		var sourceCode = Stream.of("package eu.solven.cleanthat.do_not_format_me;",
				"",
				"import java.time.LocalDate;",
				"import java.time.LocalDateTime;",
				"",
				"public class CleanClass {",
				"",
				"",
				"",
				"",
				"",
				"",
				"	final LocalDate someLocalDate;",
				"",
				"	final LocalDateTime someLocalDateTime;",
				"",
				"	public CleanClass(LocalDate someLocalDate, LocalDateTime someLocalDateTime) {",
				"		super();",
				"		this.someLocalDate = someLocalDate;",
				"		this.someLocalDateTime = someLocalDateTime;",
				"	}",
				"}",
				"",
				"").collect(Collectors.joining(System.lineSeparator()));

		var expectedCleaned = Stream.of("/* (C)2023 */",
				"package eu.solven.cleanthat.do_not_format_me;",
				"",
				"import java.time.LocalDate;",
				"import java.time.LocalDateTime;",
				"",
				"public class CleanClass {",
				"",
				"	final LocalDate someLocalDate;",
				"",
				"	final LocalDateTime someLocalDateTime;",
				"",
				"	public CleanClass(LocalDate someLocalDate, LocalDateTime someLocalDateTime) {",
				"		super();",
				"		this.someLocalDate = someLocalDate;",
				"		this.someLocalDateTime = someLocalDateTime;",
				"	}",
				"}",
				"").collect(Collectors.joining(System.lineSeparator()));

		var languageP = getEngineProperties();

		EngineAndLinters compile = helper.compile(languageP, cleanthatSession, formatter);
		var contentPath = CleanthatPathHelpers.makeContentPath(cleanthatSession.getRepositoryRoot(),
				"someModule/src/main/java/some_package/someFilePath.java");
		var cleaned = applier.applyProcessors(compile, new PathAndContent(contentPath, sourceCode));
		Assert.assertEquals(expectedCleaned, cleaned);
	}

	@Ignore("https://github.com/diffplug/spotless/issues/1555")
	@Test
	public void testCompileManyTimes() throws JsonParseException, JsonMappingException, IOException {

		var sourceCode = Stream.of("package eu.solven.cleanthat.do_not_format_me;",
				"",
				"import java.time.LocalDate;",
				"import java.time.LocalDateTime;",
				"",
				"public class CleanClass {",
				"",
				"",
				"",
				"",
				"",
				"",
				"	final LocalDate someLocalDate;",
				"",
				"	final LocalDateTime someLocalDateTime;",
				"",
				"	public CleanClass(LocalDate someLocalDate, LocalDateTime someLocalDateTime) {",
				"		super();",
				"		this.someLocalDate = someLocalDate;",
				"		this.someLocalDateTime = someLocalDateTime;",
				"	}",
				"}",
				"",
				"").collect(Collectors.joining(System.lineSeparator()));
		var languageP = getEngineProperties();
		var contentPath = CleanthatPathHelpers.makeContentPath(cleanthatSession.getRepositoryRoot(),
				"someModule/src/main/java/some_package/someFilePath.java");

		while (true) {
			EngineAndLinters compile = helper.compile(languageP, cleanthatSession, formatter);
			applier.applyProcessors(compile, new PathAndContent(contentPath, sourceCode));

			compile.close();
		}
	}
}
