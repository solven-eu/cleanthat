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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import eu.solven.cleanthat.code_provider.local.FileSystemGitCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.EngineInitializerResult;
import eu.solven.cleanthat.config.GenerateInitialConfig;
import eu.solven.cleanthat.config.ICleanthatConfigConstants;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.config.spring.ConfigSpringConfig;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.lambda.AllEnginesSpringConfig;

/**
 * This mojo will generate a relevant cleanthat configuration in current folder
 * 
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
@Mojo(name = CleanThatInitMojo.MOJO_INIT,
		// This would be called once and for all
		defaultPhase = LifecyclePhase.NONE,
		threadSafe = true,
		// One may rely on the mvn plugin to initialize a configuration, even if no pom.xml is available
		requiresProject = false)
public class CleanThatInitMojo extends ACleanThatSpringMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatInitMojo.class);

	public static final String MOJO_INIT = "init";

	@Override
	protected List<Class<?>> springClasses() {
		List<Class<?>> classes = new ArrayList<>();

		classes.add(ConfigSpringConfig.class);
		classes.add(CodeProviderHelpers.class);

		// Needed to generate default configuration given all knowns languages
		classes.add(AllEnginesSpringConfig.class);

		return classes;
	}

	@Override
	public void doClean(ApplicationContext appContext) throws MojoFailureException {
		// https://github.com/maven-download-plugin/maven-download-plugin/blob/master/src/main/java/com/googlecode/download/maven/plugin/internal/WGet.java#L324
		if (isRunOnlyAtRoot()) {
			if (getProject().isExecutionRoot()) {
				getLog().debug("We are, as expected, at executionRoot");
			} else {
				// This will check it is called only if the command is run from the project root.
				// However, it will not prevent the plugin to be called on each module
				getLog().info("maven-cleanthat-plugin:cleanthat skipped (not project root)");
				return;
			}
		} else {
			getLog().debug("Not required to be executed at root");
		}

		String configPath = getRepositoryConfigPath();
		if (Strings.isNullOrEmpty(configPath)) {
			throw new IllegalArgumentException("We need a not-empty configPath to run the 'init' mojo");
		}

		getLog().info("Path: " + configPath);

		Path configPathFile = Paths.get(configPath);

		if (!checkIfValidToInit(configPathFile)) {
			throw new MojoFailureException(configPathFile,
					"Configuration cannot be generated",
					"Something prevents the generation of a configuration");
		}

		ICodeProvider codeProvider = new FileSystemGitCodeProvider(getBaseDir().toPath());

		GenerateInitialConfig generateInitialConfig =
				new GenerateInitialConfig(appContext.getBeansOfType(IEngineLintFixerFactory.class).values());
		EngineInitializerResult properties;
		try {
			properties = generateInitialConfig.prepareDefaultConfiguration(codeProvider);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue preparing initial config given codeProvider=" + codeProvider, e);
		}
		writeConfiguration(configPathFile, properties.getRepoProperties());

		// Prefix with '.' to convert from absolute path (in the Git repository) to relative path (in the FileSystem
		// root directory)
		properties.getPathToContents()
				.forEach((path, content) -> writeFile(getBaseDir().toPath().resolve("." + path), content));
	}

	public boolean checkIfValidToInit(Path configPathFile) {
		boolean isValid = true;

		MavenProject project = getProject();
		if (project == null || project.getBasedir() == null) {
			// This happens on folder which has no pom.xml
			// Useful to projects not integrating maven, but wishing to be initialized through the mvn plugin
			LOGGER.info("You are initializing cleanthat without a pom.xml to contextualize it");
		} else {
			File baseFir = project.getBasedir();

			Path relativized = baseFir.toPath().relativize(configPathFile);

			String expectedConfigPath = ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT;

			if (!relativized.toString().equals(expectedConfigPath)) {
				LOGGER.info("We'll init only in a module containing the configuration in relative path {}",
						baseFir.toPath().resolve(ICleanthatConfigConstants.PATHES_CLEANTHAT.get(0)));
				isValid = false;
			}
		}

		if (configPathFile.toFile().isDirectory()) {
			LOGGER.error("The path of the configuration is a folder: '{}'", configPathFile);
			isValid = false;
		} else if (configPathFile.toFile().exists()) {
			if (configPathFile.toFile().isFile()) {
				LOGGER.error("There is already a configuration: '{}'", configPathFile);
			} else {
				LOGGER.error("There is something but not a file at configuration: '{}'", configPathFile);
			}
			isValid = false;
		} else {
			LOGGER.info("We are about to init a configuration at: '{}'", configPathFile);
		}
		return isValid;
	}

	public void writeConfiguration(Path configPathFile, CleanthatRepositoryProperties properties) {
		ObjectMapper yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();
		String asYaml;
		try {
			asYaml = yamlObjectMapper.writeValueAsString(properties);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Issue converting " + properties + " to YAML", e);
		}

		writeFile(configPathFile, asYaml);
	}

	private void writeFile(Path configPathFile, String content) {
		if (configPathFile.toFile().getParentFile().mkdirs()) {
			LOGGER.info("We created parent folder(s) for {}", configPathFile);
		}

		try {
			// StandardOpenOption.TRUNCATE_EXISTING
			Files.writeString(configPathFile, content, Charsets.UTF_8, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue writing content into: " + configPathFile, e);
		}
	}
}
