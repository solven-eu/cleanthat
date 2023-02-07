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
package eu.solven.cleanthat.spotless;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.maven.resolver.examples.util.Booter;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.GitAttributesLineEndings_InMemory;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.spotless.language.JavaFormatterFactory;
import eu.solven.cleanthat.spotless.language.JsonFormatterFactory;
import eu.solven.cleanthat.spotless.language.MarkdownFormatterFactory;
import eu.solven.cleanthat.spotless.language.PomXmlFormatterFactory;
import eu.solven.cleanthat.spotless.language.ScalaFormatterFactory;
import eu.solven.cleanthat.spotless.language.XmlFormatterFactory;
import eu.solven.cleanthat.spotless.mvn.ArtifactResolver;
import eu.solven.cleanthat.spotless.mvn.MavenProvisioner;
import eu.solven.cleanthat.spotless.pojo.SpotlessEngineProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;

/**
 * Knows how to instantiate {@link AFormatterStepFactory}
 * 
 * @author Benoit Lacelle
 *
 */
public class FormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatterFactory.class);

	private static final String ID_JSON = "json";
	private static final String ID_YAML = "yaml";
	private static final String ID_XML = "xml";
	private static final String ID_POM = "pom";
	private static final String ID_MARKDOWN = "markdown";
	private static final String ID_SCALA = "scala";
	public static final String ID_JAVA = "java";

	private static final AtomicReference<Path> REF_LOCALREPO = new AtomicReference<>();

	final FileSystem fileSystem;
	final ICodeProvider codeProvider;

	public FormatterFactory(CleanthatSession cleanthatSession) {
		this.fileSystem = cleanthatSession.getFileSystem();
		this.codeProvider = cleanthatSession.getCodeProvider();
	}

	public static Provisioner makeProvisioner() throws IOException {
		RepositorySystem repositorySystem = Booter.newRepositorySystem(Booter.selectFactory(new String[0]));

		// This means each Lambda will download its own jars (wtill sharing JARs through executions within the same
		// JVM/Lambda instance)
		if (REF_LOCALREPO.get() == null) {
			REF_LOCALREPO.compareAndSet(null, Files.createTempDirectory("cleanthat-spotless-m2repository"));
			LOGGER.info("We initialized local m2repository: {}", REF_LOCALREPO.get());
		} else {
			LOGGER.info("We re-use local m2repository: {}", REF_LOCALREPO.get());
		}
		DefaultRepositorySystemSession repositorySystemSession =
				Booter.newRepositorySystemSession(repositorySystem, REF_LOCALREPO.get());

		Provisioner provisionner = MavenProvisioner.create(new ArtifactResolver(repositorySystem,
				repositorySystemSession,
				Booter.newRepositories(repositorySystem, repositorySystemSession)));
		return provisionner;
	}

	public static Set<String> getFormatterIds() {
		return ImmutableSet.of(ID_JAVA, ID_SCALA, ID_MARKDOWN, ID_POM, ID_XML, ID_JSON, ID_YAML);
	}

	public static Set<String> getDefaultIncludes() {
		return getFormatterIds().stream()
				.map(s -> SpotlessFormatterProperties.builder().format(s).build())
				.map(s -> makeFormatterFactory(s))
				.flatMap(f -> f.defaultIncludes().stream())
				// Spotless patterns are always implicitly glob
				.map(s -> "glob:" + s)
				.collect(Collectors.toSet());
	}

	public static AFormatterFactory makeFormatterFactory(SpotlessFormatterProperties spotlessProperties) {
		String format = spotlessProperties.getFormat();
		switch (format) {
		case ID_JAVA:
			return new JavaFormatterFactory();
		case ID_SCALA:
			return new ScalaFormatterFactory();
		case ID_MARKDOWN:
			return new MarkdownFormatterFactory();
		case ID_POM:
			return new PomXmlFormatterFactory();
		case ID_XML:
			return new XmlFormatterFactory();
		case ID_JSON:
			return new JsonFormatterFactory();
		case ID_YAML:
			return new JsonFormatterFactory();

		default:
			throw new IllegalArgumentException("Not managed format: " + format);
		}
	}

	public AFormatterStepFactory makeFormatterStepFactory(SpotlessFormatterProperties spotlessProperties) {
		return makeFormatterFactory(spotlessProperties).makeStepFactory(codeProvider, spotlessProperties);
	}

	// com.diffplug.gradle.spotless.SpotlessTask#buildFormatter
	public EnrichedFormatter makeFormatter(SpotlessEngineProperties engineProperties,
			SpotlessFormatterProperties formatterProperties,
			Provisioner provisioner) {
		// In our virtual fileSystem, we process from the root (as root of the repository)
		Path tmpRoot = fileSystem.getPath("/");

		// File baseDir;
		// Supplier<Iterable<File>> filesProvider;
		LineEnding.Policy lineEndingsPolicy;
		LineEnding lineEnding = LineEnding.valueOf(engineProperties.getLineEnding());
		if (lineEnding == LineEnding.GIT_ATTRIBUTES) {
			// LineEnding.createPolicy(File, Supplier<Iterable<File>>) is file-system oriented
			lineEndingsPolicy = GitAttributesLineEndings_InMemory.create(codeProvider,
					engineProperties.getGit(),
					fileSystem.getPath("/"),
					() -> Collections.emptyList());
		} else {
			lineEndingsPolicy = lineEnding.createPolicy();
		}

		// FormatExceptionPolicy.failOnlyOnError()
		FormatExceptionPolicy exceptionPolicy = new FormatExceptionPolicyStrict();

		String encoding = formatterProperties.getEncoding();
		if (encoding == null) {
			encoding = engineProperties.getEncoding();
		}
		AFormatterStepFactory stepFactory = makeFormatterStepFactory(formatterProperties);

		List<FormatterStep> steps = buildSteps(stepFactory, formatterProperties, provisioner);
		return new EnrichedFormatter(stepFactory,
				Formatter.builder()
						.lineEndingsPolicy(lineEndingsPolicy)
						.encoding(Charset.forName(encoding))
						.rootDir(tmpRoot)
						.steps(steps)
						.exceptionPolicy(exceptionPolicy)
						.build());
	}

	private List<FormatterStep> buildSteps(AFormatterStepFactory stepFactory,
			SpotlessFormatterProperties spotlessProperties,
			Provisioner provisioner) {
		return spotlessProperties.getSteps()
				.stream()
				.map(s -> stepFactory.makeStep(s, provisioner))
				.collect(Collectors.toList());
	}
}
