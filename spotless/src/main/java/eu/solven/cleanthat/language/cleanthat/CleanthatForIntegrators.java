/*
 * Copyright 2023-2026 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.language.cleanthat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.diffplug.common.annotations.VisibleForTesting;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.config.pojo.ICleanthatStepParametersProperties;
import eu.solven.pepper.collection.PepperMapHelper;
import eu.solven.pepper.resource.PepperResourceHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * Helps integrating CleanThat
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class CleanthatForIntegrators {

	public static final String ENV_CLEANTHAT_INCLUDE_DRAFT = "cleanthat.include_draft";

	// The version of cleanthat to use when the proper-version detection mechanism fails
	private static final String DEFAULT_DEFAULT_CLEANTHAT_VERSION = "2.10";
	private static final String RESOURCE_MAVEN_JSON = "/maven.json";

	// The default version of cleanthat to use. When running in PRD, it may typically be the previous RELEASE version
	// (if the checked-out code is -SNAPSHOT), .
	private static final String DEFAULT_CLEANTHAT_VERSION;

	public static final List<String> DEFAULT_MUTATORS;

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

		DEFAULT_CLEANTHAT_VERSION = parseCleanthatDefaultVersion();
	}

	protected CleanthatForIntegrators() {
		// hidden
	}

	private static String parseCleanthatDefaultVersion() {
		var mavenProperties = PepperResourceHelper.loadAsString(RESOURCE_MAVEN_JSON, StandardCharsets.UTF_8);
		Map<?, ?> asMap;
		try {
			asMap = new ObjectMapper().readValue(mavenProperties, Map.class);
		} catch (JsonProcessingException e) {
			LOGGER.error("Issue loading from {}. Fallback on {}", RESOURCE_MAVEN_JSON, DEFAULT_CLEANTHAT_VERSION, e);
			return DEFAULT_DEFAULT_CLEANTHAT_VERSION;
		}

		var cleanthatVersion = cleanCleanthatVersionFromMvnProperties(asMap);

		return cleanthatVersion;
	}

	@VisibleForTesting
	static String cleanCleanthatVersionFromMvnProperties(Map<?, ?> asMap) {
		String cleanthatVersion;
		var rawMavenProjectVersion = PepperMapHelper.getRequiredString(asMap, "project.version");
		if (rawMavenProjectVersion.startsWith("@")) {
			rawMavenProjectVersion = DEFAULT_DEFAULT_CLEANTHAT_VERSION;
			cleanthatVersion = rawMavenProjectVersion;
			LOGGER.error("Issue loading from {} (as we found {}). Fallback on {}",
					RESOURCE_MAVEN_JSON,
					rawMavenProjectVersion,
					cleanthatVersion);
		} else {
			if (rawMavenProjectVersion.endsWith("-SNAPSHOT")) {
				if ("true".equals(System.getProperty(ENV_CLEANTHAT_INCLUDE_DRAFT))) {
					LOGGER.info("Spotless will execute a '-SNAPSHOT' of cleanthat");
					cleanthatVersion = rawMavenProjectVersion;
				} else {
					LOGGER.info("Spotless will execute a '-RELEASE' of cleanthat");
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

	public static String getDefaultVersion() {
		return DEFAULT_CLEANTHAT_VERSION;
	}

	public static List<String> getDefaultMutators() {
		return DEFAULT_MUTATORS;
	}
}
