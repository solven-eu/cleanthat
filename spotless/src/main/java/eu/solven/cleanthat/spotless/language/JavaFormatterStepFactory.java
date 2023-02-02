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

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.diffplug.spotless.java.ImportOrderStep;
import com.diffplug.spotless.java.RemoveUnusedImportsStep;
import com.google.common.collect.ImmutableList;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
					+ "eclipse_formatter-stylesheet.xml";

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
		switch (stepId) {
		case "removeUnusedImports": {
			return RemoveUnusedImportsStep.create(provisioner);
		}
		case "importOrder": {
			return makeImportOrder(s);
		}
		case "eclipse": {
			return makeEclipse(s, provisioner);
		}
		default: {
			throw new IllegalArgumentException("Unknown Java step: " + stepId);
		}
		}
	}

	private FormatterStep makeEclipse(SpotlessStepProperties s, Provisioner provisioner) {
		EclipseBasedStepBuilder eclipseConfig = EclipseJdtFormatterStep.createBuilder(provisioner);

		String eclipseVersion = s.getCustomProperty("version", String.class);
		if (eclipseVersion == null) {
			eclipseVersion = EclipseJdtFormatterStep.defaultVersion();
		}
		eclipseConfig.setVersion(eclipseVersion);

		String stylesheetFile = s.getCustomProperty(KEY_ECLIPSE_FILE, String.class);
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

	private FormatterStep makeImportOrder(SpotlessStepProperties s) {
		// https://stackoverflow.com/questions/34450900/how-to-sort-import-statements-in-eclipse-in-case-insensitive-order
		Boolean wildcardsLast = s.getCustomProperty("wildcardsLast", Boolean.class);
		if (wildcardsLast == null) {
			// https://github.com/diffplug/spotless/tree/main/plugin-maven#java
			wildcardsLast = false;
		}

		String ordersFile = s.getCustomProperty(KEY_FILE, String.class);
		if (ordersFile != null) {
			return ImportOrderStep.forJava().createFrom(wildcardsLast, ordersFile);
		}

		// You can use an empty string for all the imports you didn't specify explicitly, '|' to join group without
		// blank line, and '\#` prefix for static imports.
		String ordersString = s.getCustomProperty("order", String.class);
		if (ordersString == null) {
			// The default eclipse configuration
			ordersString = Stream.of("java", "javax", "org", "com").collect(Collectors.joining(","));
		}
		return ImportOrderStep.forJava().createFrom(wildcardsLast, ordersString.split(","));
	}

	// This is useful to demonstrate all available configuration
	public static List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties removeUnusedImports = new SpotlessStepProperties();
		removeUnusedImports.setId("removeUnusedImports");

		SpotlessStepProperties importOrder = new SpotlessStepProperties();
		importOrder.setId("importOrder");
		importOrder.putProperty(KEY_FILE, "repository:/.cleanthat/java-importOrder.properties");

		SpotlessStepProperties eclipse = new SpotlessStepProperties();
		eclipse.setId(ID_ECLIPSE);
		eclipse.putProperty("version", EclipseJdtFormatterStep.defaultVersion());
		eclipse.putProperty(KEY_FILE, "repository:/.cleanthat/java-eclipse_stylesheet.xml");

		return ImmutableList.<SpotlessStepProperties>builder()
				.add(removeUnusedImports)
				.add(importOrder)
				.add(eclipse)
				.build();
	}

	public static SpotlessStepProperties makeDefaultEclipseStep() {
		SpotlessStepProperties eclipseStep = new SpotlessStepProperties();
		eclipseStep.setId(JavaFormatterStepFactory.ID_ECLIPSE);
		eclipseStep.putProperty(JavaFormatterStepFactory.KEY_ECLIPSE_FILE,
				CleanthatUrlLoader.PREFIX_CODE + JavaFormatterStepFactory.DEFAULT_ECLIPSE_FILE);

		return eclipseStep;
	}

}
