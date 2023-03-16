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
package eu.solven.cleanthat.spotless.language;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.common.annotations.VisibleForTesting;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.diffplug.spotless.java.CleanthatJavaStep;
import com.diffplug.spotless.java.ImportOrderStep;
import com.diffplug.spotless.java.RemoveUnusedImportsStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.ICleanthatConfigConstants;
import eu.solven.cleanthat.config.pojo.ICleanthatStepParametersProperties;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import eu.solven.pepper.collection.PepperMapHelper;
import eu.solven.pepper.resource.PepperResourceHelper;

/**
 * Configure Spotless engine for '.java' files
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaFormatterStepFactory extends AFormatterStepFactory {
	static final String ENV_CLEANTHAT_INCLUDE_DRAFT = "cleanthat.include_draft";

	private static final Logger LOGGER = LoggerFactory.getLogger(JavaFormatterStepFactory.class);

	private static final String DEFAULT_CLEANTHAT_VERSION = "2.10";
	private static final String RESOURCE_MAVEN_JSON = "/maven.json";

	static final String KEY_ORDER = "order";
	// The default eclipse configuration
	static final String ORDER_DEFAULT_ECLIPSE =
			Stream.of("java", "javax", "org", "com").collect(Collectors.joining(","));

	public static final List<String> DEFAULT_MUTATORS;

	private static final String LICENSE_HEADER_DELIMITER = "package ";

	public static final String KEY_ECLIPSE_FILE = KEY_FILE;

	public static final String DEFAULT_ECLIPSE_FILE =
			ICleanthatConfigConstants.PATH_SEPARATOR + ICleanthatConfigConstants.FILENAME_CLEANTHAT_FOLDER
					+ ICleanthatConfigConstants.PATH_SEPARATOR
					+ "eclipse_java-stylesheet.xml";

	public static final String ID_ECLIPSE = "eclipse";

	private static final String CLEANTHAT_VERSION;

	static {
		ImmutableList.Builder<String> defaultMutatorsBuilder =
				ImmutableList.<String>builder().add(ICleanthatStepParametersProperties.SAFE_AND_CONSENSUAL);
		if ("true".equals(System.getProperty(ENV_CLEANTHAT_INCLUDE_DRAFT))) {
			LOGGER.warn("We include {} in default mutators",
					ICleanthatStepParametersProperties.SAFE_BUT_NOT_CONSENSUAL);
			defaultMutatorsBuilder.add(ICleanthatStepParametersProperties.SAFE_BUT_NOT_CONSENSUAL);
			defaultMutatorsBuilder.add(ICleanthatStepParametersProperties.SAFE_BUT_CONTROVERSIAL);
		}

		DEFAULT_MUTATORS = defaultMutatorsBuilder.build();

		CLEANTHAT_VERSION = parseCleanthatDefaultVersion();
	}

	private static String parseCleanthatDefaultVersion() {
		var mavenProperties = PepperResourceHelper.loadAsString(RESOURCE_MAVEN_JSON, StandardCharsets.UTF_8);
		Map<?, ?> asMap;
		try {
			asMap = new ObjectMapper().readValue(mavenProperties, Map.class);
		} catch (JsonProcessingException e) {
			LOGGER.error("Issue loading from {}. Fallback on {}", RESOURCE_MAVEN_JSON, CLEANTHAT_VERSION, e);
			return DEFAULT_CLEANTHAT_VERSION;
		}

		var cleanthatVersion = cleanCleanthatVersionFromMvnProperties(asMap);

		return cleanthatVersion;
	}

	@VisibleForTesting
	static String cleanCleanthatVersionFromMvnProperties(Map<?, ?> asMap) {
		String cleanthatVersion;
		var rawMavenProjectVersion = PepperMapHelper.getRequiredString(asMap, "project.version");
		if (rawMavenProjectVersion.startsWith("@")) {
			rawMavenProjectVersion = DEFAULT_CLEANTHAT_VERSION;
			cleanthatVersion = rawMavenProjectVersion;
			LOGGER.error("Issue loading from {} (as we found {}). Fallback on {}",
					RESOURCE_MAVEN_JSON,
					rawMavenProjectVersion,
					cleanthatVersion);
		} else {
			if (rawMavenProjectVersion.endsWith("-SNAPSHOT")) {
				if ("true".equals(System.getProperty(ENV_CLEANTHAT_INCLUDE_DRAFT))) {
					LOGGER.info("Spotless will execute a '-SNAPSHOT' of clenathat");
					cleanthatVersion = rawMavenProjectVersion;
				} else {
					LOGGER.info("Spotless will execute a '-RELEASE' of clenathat");
					cleanthatVersion = getPreviousRelease(rawMavenProjectVersion);
				}
			} else {
				cleanthatVersion = rawMavenProjectVersion;
			}

			LOGGER.info("We are running over cleanthat.version={}", cleanthatVersion);
		}
		return cleanthatVersion;
	}

	private static String getPreviousRelease(String rawMavenProjectVersion) {
		if (!rawMavenProjectVersion.endsWith("-SNAPSHOT")) {
			throw new IllegalArgumentException(rawMavenProjectVersion + " should be a -SNAPSHOT");
		}

		var lastIndexOfDot = rawMavenProjectVersion.lastIndexOf('.');

		var snapshotMinorVersion = rawMavenProjectVersion.substring(lastIndexOfDot + 1,
				rawMavenProjectVersion.length() - "-SNAPSHOT".length());

		return rawMavenProjectVersion.substring(0, lastIndexOfDot + 1) + (Integer.parseInt(snapshotMinorVersion) - 1);
	}

	public JavaFormatterStepFactory(JavaFormatterFactory formatterFactory,
			ICodeProvider codeProvider,
			SpotlessFormatterProperties formatterProperties) {
		super(formatterFactory, codeProvider, formatterProperties);
	}

	@Override
	public String licenseHeaderDelimiter() {
		return LICENSE_HEADER_DELIMITER;
	}

	@Override
	public FormatterStep makeSpecializedStep(SpotlessStepProperties s, Provisioner provisioner) {
		String stepId = s.getId();
		SpotlessStepParametersProperties parameters = s.getParameters();

		switch (stepId) {
		case "removeUnusedImports": {
			return RemoveUnusedImportsStep.create(provisioner);
		}
		case "importOrder": {
			return makeImportOrder(parameters);
		}
		case ID_ECLIPSE: {
			return makeEclipse(parameters, provisioner);
		}
		case "cleanthat": {
			return makeCleanthat(parameters, provisioner);
		}
		default: {
			throw new IllegalArgumentException("Unknown Java step: " + stepId);
		}
		}
	}

	private FormatterStep makeCleanthat(SpotlessStepParametersProperties parameters, Provisioner provisioner) {
		String cleanthatVersion = parameters.getCustomProperty("version", String.class);
		if (cleanthatVersion == null) {
			cleanthatVersion = CLEANTHAT_VERSION;
		}

		String sourceJdk = parameters.getCustomProperty("source_jdk", String.class);
		if (sourceJdk == null) {
			throw new IllegalArgumentException(
					"The property spotless.java.cleanthat.source_jdk is mandatory (e.g. set it to '1.8' or '11')");
		}

		List<String> mutators = parameters.getCustomProperty("mutators", List.class);
		if (mutators == null || mutators.isEmpty()) {
			mutators = DEFAULT_MUTATORS;
		}

		List<String> excludedMutators = parameters.getCustomProperty("excluded_mutators", List.class);

		Boolean includeDraft = parameters.getCustomProperty("include_draft", Boolean.class);
		if (includeDraft == null) {
			includeDraft = false;
		}

		var defaultGroupArtifact = CleanthatJavaStep.defaultGroupArtifact();
		return CleanthatJavaStep.create(defaultGroupArtifact,
				cleanthatVersion,
				sourceJdk,
				mutators,
				excludedMutators,
				includeDraft,
				provisioner);
	}

	private FormatterStep makeEclipse(SpotlessStepParametersProperties parameters, Provisioner provisioner) {
		EclipseBasedStepBuilder eclipseConfig = EclipseJdtFormatterStep.createBuilder(provisioner);

		String eclipseVersion = parameters.getCustomProperty("version", String.class);
		if (eclipseVersion == null) {
			eclipseVersion = EclipseJdtFormatterStep.defaultVersion();
		}
		eclipseConfig.setVersion(eclipseVersion);

		String stylesheetFile = parameters.getCustomProperty(KEY_ECLIPSE_FILE, String.class);
		if (stylesheetFile != null) {
			File settingsFile;
			try {
				settingsFile = locateFile(stylesheetFile);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue processing eclipse.file: " + stylesheetFile, e);
			}
			eclipseConfig.setPreferences(Arrays.asList(settingsFile));
		}
		return eclipseConfig.build();
	}

	private FormatterStep makeImportOrder(SpotlessStepParametersProperties parameters) {
		// https://stackoverflow.com/questions/34450900/how-to-sort-import-statements-in-eclipse-in-case-insensitive-order
		Boolean wildcardsLast = parameters.getCustomProperty("wildcardsLast", Boolean.class);
		if (wildcardsLast == null) {
			// https://github.com/diffplug/spotless/tree/main/plugin-maven#java
			wildcardsLast = false;
		}

		String ordersFile = parameters.getCustomProperty(KEY_FILE, String.class);
		if (ordersFile != null) {
			File orderFileAsFile;
			try {
				orderFileAsFile = locateFile(ordersFile);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue locating " + ordersFile, e);
			}
			return ImportOrderStep.forJava().createFrom(wildcardsLast, orderFileAsFile);
		}

		// You can use an empty string for all the imports you didn't specify explicitly, '|' to join group without
		// blank line, and '\#` prefix for static imports.
		String ordersString = parameters.getCustomProperty(KEY_ORDER, String.class);
		if (ordersString == null) {
			ordersString = ORDER_DEFAULT_ECLIPSE;
		}
		return ImportOrderStep.forJava().createFrom(wildcardsLast, ordersString.split(","));
	}

}
