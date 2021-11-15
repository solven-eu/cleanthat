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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;

/**
 * This mojo will generate a relevant cleanthat configuration in current folder
 * 
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
@Mojo(name = "init",
		// This would be called once and for all
		defaultPhase = LifecyclePhase.NONE,
		threadSafe = true)
public class CleanThatInitMojo extends ACleanThatSpringMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatInitMojo.class);

	@Override
	protected List<Class<?>> springClasses() {
		List<Class<?>> classes = new ArrayList<>();

		classes.add(CodeProviderHelpers.class);

		return classes;
	}

	@Override
	public void doClean(ApplicationContext appContext) {
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

		String configPath = getConfigPath();
		if (Strings.isNullOrEmpty(configPath)) {
			throw new IllegalArgumentException("We need a not-empty configPath to run the 'init' mojo");
		}

		getLog().info("Path: " + configPath);

		Path configPathFile = Paths.get(configPath);
		File baseFir = getProject().getBasedir();

		Path configPathFileParent = configPathFile.getParent();
		if (!configPathFileParent.equals(baseFir.toPath())) {
			LOGGER.info("We'll init only in a module containing the configuration at its root: {}",
					configPathFileParent);
			return;
		}

		if (configPathFile.toFile().isDirectory()) {
			LOGGER.error("The path of the configuration is a folder: '{}'", configPathFile);
			return;
		} else if (configPathFile.toFile().exists()) {
			if (configPathFile.toFile().isFile()) {
				LOGGER.error("There is already a configuration: '{}'", configPathFile);
				return;
			} else {
				LOGGER.error("There is something but not a file at configuration: '{}'", configPathFile);
				return;
			}
		} else {
			LOGGER.info("We are about to init a configuration at: '{}'", configPathFile);
		}

		CleanthatRepositoryProperties properties = new CleanthatRepositoryProperties();

		writeConfiguration(configPathFile, properties);
	}

	public void writeConfiguration(Path configPathFile, CleanthatRepositoryProperties properties) {
		ObjectMapper yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();
		String asYaml;
		try {
			asYaml = yamlObjectMapper.writeValueAsString(properties);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Issue converting " + properties + " to YAML", e);
		}

		try {
			// StandardOpenOption.TRUNCATE_EXISTING
			Files.writeString(configPathFile, asYaml, Charsets.UTF_8, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue writing YAML into: " + configPathFile, e);
		}
	}
}
