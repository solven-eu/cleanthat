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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.diffplug.spotless.java.CleanthatJavaStep;
import com.diffplug.spotless.java.ImportOrderStep;
import com.diffplug.spotless.java.RemoveUnusedImportsStep;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;

/**
 * Configure Spotless engine for '.java' files
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaFormatterStepFactory extends AFormatterStepFactory {
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	public static final String KEY_ECLIPSE_FILE = KEY_FILE;

	public static final String DEFAULT_ECLIPSE_FILE =
			CodeProviderHelpers.PATH_SEPARATOR + CodeProviderHelpers.FILENAME_CLEANTHAT_FOLDER
					+ CodeProviderHelpers.PATH_SEPARATOR
					+ "eclipse_java-stylesheet.xml";

	public static final String ID_ECLIPSE = "eclipse";

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
		case "eclipse": {
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
			cleanthatVersion = CleanthatJavaStep.defaultVersion();
		}

		String sourceJdk = parameters.getCustomProperty("source_jdk", String.class);
		if (sourceJdk == null) {
			throw new IllegalArgumentException(
					"The property spotless.java.cleanthat.source_jdk is mandatory (e.g. set it to '1.8' or '11')");
		}

		List<String> mutators = parameters.getCustomProperty("mutators", List.class);
		List<String> excludedMutators = parameters.getCustomProperty("excluded_mutators", List.class);
		String defaultGroupArtifact = CleanthatJavaStep.defaultGroupArtifact();
		return CleanthatJavaStep
				.create(defaultGroupArtifact, cleanthatVersion, sourceJdk, mutators, excludedMutators, provisioner);
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
		String ordersString = parameters.getCustomProperty("order", String.class);
		if (ordersString == null) {
			// The default eclipse configuration
			ordersString = Stream.of("java", "javax", "org", "com").collect(Collectors.joining(","));
		}
		return ImportOrderStep.forJava().createFrom(wildcardsLast, ordersString.split(","));
	}

	// This is useful to demonstrate all available configuration
	public static List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties removeUnusedImports = SpotlessStepProperties.builder().id("removeUnusedImports").build();

		SpotlessStepProperties importOrder = SpotlessStepProperties.builder().id("importOrder").build();
		SpotlessStepParametersProperties importOrderParameters = new SpotlessStepParametersProperties();
		importOrderParameters.putProperty(KEY_FILE, "repository:/.cleanthat/java-importOrder.properties");
		importOrder.setParameters(importOrderParameters);

		// Cleanthat before Eclipse as CleanThat may break the style
		SpotlessStepProperties cleanthat = SpotlessStepProperties.builder().id(ID_ECLIPSE).build();
		SpotlessStepParametersProperties cleanthatParameters = new SpotlessStepParametersProperties();
		cleanthatParameters.putProperty(KEY_FILE, "11");
		cleanthat.setParameters(cleanthatParameters);

		SpotlessStepProperties eclipse = SpotlessStepProperties.builder().id(ID_ECLIPSE).build();
		SpotlessStepParametersProperties eclipseParameters = new SpotlessStepParametersProperties();
		eclipseParameters.putProperty("version", EclipseJdtFormatterStep.defaultVersion());
		eclipseParameters.putProperty(KEY_FILE,
				CleanthatUrlLoader.PREFIX_CODE + JavaFormatterStepFactory.DEFAULT_ECLIPSE_FILE);
		eclipse.setParameters(eclipseParameters);

		return ImmutableList.<SpotlessStepProperties>builder()
				.add(removeUnusedImports)
				.add(importOrder)
				.add(eclipse)
				.build();
	}

	public static SpotlessStepProperties makeDefaultEclipseStep() {
		SpotlessStepProperties eclipse = SpotlessStepProperties.builder().id(ID_ECLIPSE).build();
		SpotlessStepParametersProperties eclipseParameters = new SpotlessStepParametersProperties();
		eclipseParameters.putProperty(JavaFormatterStepFactory.KEY_ECLIPSE_FILE,
				CleanthatUrlLoader.PREFIX_CODE + JavaFormatterStepFactory.DEFAULT_ECLIPSE_FILE);
		eclipse.setParameters(eclipseParameters);

		return eclipse;
	}

}
