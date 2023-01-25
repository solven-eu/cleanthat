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
package eu.solven.cleanthat.spotless;

import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.GitAttributesLineEndings_InMemory;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.spotless.language.JavaFormatterStepFactory;
import eu.solven.cleanthat.spotless.language.PomXmlFormatterStepFactory;
import eu.solven.cleanthat.spotless.mvn.ArtifactResolver;
import eu.solven.cleanthat.spotless.mvn.MavenProvisioner;
import eu.solven.cleanthat.spotless.pojo.SpotlessEngineProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.resolver.examples.util.Booter;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Knows how to instantiate {@link AFormatterStepFactory}
 * 
 * @author Benoit Lacelle
 *
 */
public class FormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatterFactory.class);

	final FileSystem fileSystem;
	final ICodeProvider codeProvider;

	public FormatterFactory(CleanthatSession cleanthatSession) {
		this.fileSystem = cleanthatSession.getFileSystem();
		this.codeProvider = cleanthatSession.getCodeProvider();
	}

	// Provisioner provisionner = new CleanthatJvmProvisioner();
	public static Provisioner makeProvisionner() throws IOException {
		// DefaultRepositorySystem repositorySystem = new DefaultRepositorySystem();
		// DependencyCollector dependencyCollector = new DefaultDependencyCollector();
		// repositorySystem.setDependencyCollector(dependencyCollector);
		//
		// DefaultRepositorySystemSession repositorySystemSession = new DefaultRepositorySystemSession();
		// Path tmpRepo = Files.createTempDirectory("cleanthat-spotless-maven-");
		// LOGGER.info("We will use as m2 local repository: {}", tmpRepo);
		// LocalRepository localRepository = new LocalRepository(tmpRepo.toFile());
		//
		// DefaultLocalRepositoryProvider localRepositoryProvider = new DefaultLocalRepositoryProvider();
		// LocalRepositoryManagerFactory localRepositoryManagerFactory = new SimpleLocalRepositoryManagerFactory();
		// localRepositoryProvider.addLocalRepositoryManagerFactory(localRepositoryManagerFactory);
		// repositorySystem.setLocalRepositoryProvider(localRepositoryProvider);
		//
		// LocalRepositoryManager localRepositoryManager =
		// repositorySystem.newLocalRepositoryManager(repositorySystemSession, localRepository);
		// repositorySystemSession.setLocalRepositoryManager(localRepositoryManager);
		//
		// List<RemoteRepository> repositories = new ArrayList<>();
		Log log = new SystemStreamLog();

		RepositorySystem repositorySystem = Booter.newRepositorySystem(Booter.selectFactory(new String[0]));
		DefaultRepositorySystemSession repositorySystemSession = Booter.newRepositorySystemSession(repositorySystem);

		Provisioner provisionner = MavenProvisioner.create(new ArtifactResolver(repositorySystem,
				repositorySystemSession,
				Booter.newRepositories(repositorySystem, repositorySystemSession),
				log));
		return provisionner;
	}

	private AFormatterStepFactory makeFormatterStepFactory(SpotlessFormatterProperties spotlessProperties) {
		String language = spotlessProperties.getFormat();
		switch (language) {
		case "java":
			return new JavaFormatterStepFactory(codeProvider, spotlessProperties);
		case "pom":
			return new PomXmlFormatterStepFactory(codeProvider, spotlessProperties);

		default:
			throw new IllegalArgumentException("Not managed language: " + language);
		}
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

		List<FormatterStep> steps = buildSteps(stepFactory,formatterProperties, provisioner);
		return new EnrichedFormatter(stepFactory,
				Formatter.builder()
						.lineEndingsPolicy(lineEndingsPolicy)
						.encoding(Charset.forName(encoding))
						.rootDir(tmpRoot)
						.steps(steps)
						.exceptionPolicy(exceptionPolicy)
						.build());
	}

	private List<FormatterStep> buildSteps(
			AFormatterStepFactory stepFactory, SpotlessFormatterProperties spotlessProperties, Provisioner provisioner) {
		return spotlessProperties.getSteps()
				.stream()
				.map(s -> stepFactory.makeStep(s, provisioner))
				.collect(Collectors.toList());
	}
}
