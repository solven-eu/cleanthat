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
package eu.solven.cleanthat.spotless.language;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.diffplug.spotless.markdown.FlexmarkStep;
import com.diffplug.spotless.markdown.FreshMarkStep;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;

/**
 * Configure Spotless engine for 'pm.xml' files
 * 
 * @author Benoit Lacelle
 *
 */
public class MarkdownFormatterStepFactory extends AFormatterStepFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarkdownFormatterStepFactory.class);

	public MarkdownFormatterStepFactory(ICodeProvider codeProvider, SpotlessFormatterProperties spotlessProperties) {
		super(codeProvider, spotlessProperties);
	}

	// CleanThat will call spotless from the root directory: process any 'README.MD' file from there
	@Override
	public Set<String> defaultIncludes() {
		return ImmutableSet.of("**/README.MD");
	}

	@Override
	public String licenseHeaderDelimiter() {
		return null;
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public FormatterStep makeStep(SpotlessStepProperties s, Provisioner provisioner) {
		String stepName = s.getId();
		switch (stepName) {
		case "flexmark": {
			String libVersion = s.getCustomProperty("version", String.class);
			if (libVersion == null) {
				libVersion = FlexmarkStep.defaultVersion();
			}
			return FlexmarkStep.create(libVersion, provisioner);
		}
		// https://github.com/diffplug/spotless/blob/main/lib/src/main/java/com/diffplug/spotless/markdown/FreshMarkStep.java
		case "freshmark": {
			// https://github.com/diffplug/freshmark
			Object libVersion = s.getCustomProperty("version", String.class);
			if (libVersion == null) {
				libVersion = FreshMarkStep.defaultVersion();
			}

			Object rawFreshmarkProperties = s.getCustomProperty("properties", Map.class);

			Map<String, Object> properties;
			if (rawFreshmarkProperties == null) {
				properties = ImmutableMap.of();
			} else {
				properties = new LinkedHashMap<>();
				((Map) rawFreshmarkProperties).forEach((k, v) -> properties.put(String.valueOf(k), v));
			}

			return FreshMarkStep.create(libVersion.toString(), () -> properties, provisioner);
		}
		default: {
			throw new IllegalArgumentException("Unknown Java step: " + stepName);
		}
		}
	}

	// This is useful to demonstrate all available configuration
	public static List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties flexmark = new SpotlessStepProperties();
		flexmark.setId("flexmark");
		flexmark.putProperty("version", FlexmarkStep.defaultVersion());

		SpotlessStepProperties freshmark = new SpotlessStepProperties();
		freshmark.setId("freshmark");
		freshmark.putProperty("properties", Map.of("k1", "v1"));

		return ImmutableList.<SpotlessStepProperties>builder().add(flexmark).add(freshmark).build();
	}
}
