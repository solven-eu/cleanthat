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
package eu.solven.cleanthat.mvn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.any_language.ICodeCleaner;
import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ICleanthatConfigConstants;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.engine.java.JavaFormattersFactory;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerStep;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.language.cleanthat.CleanthatForIntegrators;
import eu.solven.cleanthat.language.javaparser.ICleanthatJavaparserConstants;
import eu.solven.cleanthat.mvn.codeprovider.OverlayCodeProviderWrite;

/**
 * This {@link org.apache.maven.plugin.Mojo} enables applying a single {@link IMutator} over current directory.
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
@Mojo(name = CleanThatApplyMojo.MOJO_SINGLE,
		defaultPhase = LifecyclePhase.PROCESS_SOURCES,
		threadSafe = true,
		// Used to enable symbolSolving based on project dependencies
		requiresDependencyResolution = ResolutionScope.RUNTIME,
		// One may rely on the mvn plugin to clean a folder, even if no pom.xml is available
		requiresProject = false)
@SuppressWarnings("PMD.ImmutableField")
public class CleanThatApplyMojo extends ACleanThatSpringMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatApplyMojo.class);

	public static final String MOJO_SINGLE = "apply";

	// Follow the naming from https://github.com/diffplug/spotless/tree/main/plugin-maven#cleanthat
	@Parameter(defaultValue = "${cleanthat.mutators}", required = true)
	private List<String> mutators = new ArrayList<>(CleanthatForIntegrators.getDefaultMutators());

	// Follow the naming from https://github.com/diffplug/spotless/tree/main/plugin-maven#cleanthat
	@Parameter(defaultValue = "${cleanthat.excludedMutators}")
	private List<String> excludedMutators = new ArrayList<>();

	// Follow the naming from https://github.com/diffplug/spotless/tree/main/plugin-maven#cleanthat
	// See ENV_CLEANTHAT_INCLUDE_DRAFT
	@Parameter(defaultValue = "${cleanthat.includeDraft}")
	private boolean includeDraft = false;

	@Override
	protected void checkParameters() {
		Path configPath = getMayNotExistRepositoryConfigPath();

		if (Files.exists(configPath)) {
			LOGGER.info("We apply apply a custom mutator configuration, independantly of {}", configPath);
		}
	}

	@Override
	protected List<Class<?>> springClasses() {
		if (includeDraft) {
			LOGGER.warn("Given `includeDraft==true`, we call `System.setProperty({}, \"{}\")`",
					CleanthatForIntegrators.ENV_CLEANTHAT_INCLUDE_DRAFT,
					true);
			System.setProperty(CleanthatForIntegrators.ENV_CLEANTHAT_INCLUDE_DRAFT, "true");
		}

		List<Class<?>> allClasses = new ArrayList<>();

		allClasses.addAll(CleanThatCleanThatMojo.cleanThatSpringClasses());

		// We register Javaparser engine as it is the simpler way to call a single IMutator
		// Going through Spotless requires generating addition configuration files
		allClasses.add(JavaFormattersFactory.class);

		return allClasses;
	}

	@Override
	public void doClean(ApplicationContext appContext) {
		if (isRunOnlyAtRoot() && !isThisTheExecutionRoot()) {
			// This will check it is called only if the command is run from the project root.
			// However, it will not prevent the plugin to be called on each module
			getLog().info("maven-cleanthat-plugin:" + MOJO_SINGLE + " skipped (not execution root)");
			return;
		}

		ICodeProviderWriter codeProvider = CleanThatMavenHelper.makeCodeProviderWriter(this);

		var topConfig = CleanthatRepositoryProperties.defaultRepository();

		var properties = new JavaRefactorerProperties();
		properties.setMutators(mutators);
		properties.setExcludedMutators(excludedMutators);
		properties.setIncludeDraft(includeDraft);

		var engineProperties = CleanthatEngineProperties.builder()
				.engine(ICleanthatJavaparserConstants.ENGINE_ID)
				.step(CleanthatStepProperties.builder()
						.id(JavaRefactorerStep.ID_REFACTORER)
						.parameters(properties)
						.build())
				.build();
		topConfig.setEngines(Collections.singletonList(engineProperties));

		Map<Path, String> overlays = new LinkedHashMap<>();
		ObjectMapper yamlObjectMapper = appContext.getBean(ConfigHelpers.class).getObjectMapper();
		try {
			overlays.put(
					CleanthatPathHelpers.makeContentPath(codeProvider.getRepositoryRoot(),
							ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT),
					yamlObjectMapper.writeValueAsString(topConfig));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(e);
		}

		ICodeProviderWriter overlayed = new OverlayCodeProviderWrite(codeProvider, overlays);

		ICodeCleaner codeCleaner = CleanThatMavenHelper.makeCodeCleaner(appContext);
		codeCleaner.formatCodeGivenConfig(CleanThatApplyMojo.class.getSimpleName(), overlayed, isDryRun());
	}
}
