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
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.generic.LicenseHeaderStep.YearMode;
import com.diffplug.spotless.java.ImportOrderStep;
import com.diffplug.spotless.java.RemoveUnusedImportsStep;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import eu.solven.pepper.resource.PepperResourceHelper;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;

/**
 * Configure Spotless engine for '.java' files
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaFormatterStepFactory extends AFormatterStepFactory {
	// CleanThat will call spotless from the root directory: process any Java file from there, in some 'src' parent
	// directory
	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.of("**/src/**/*.java");
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	private static final String KEY_FILE = "file";
	public static final String KEY_ECLIPSE_FILE = KEY_FILE;

	public static final String DEFAULT_ECLIPSE_FILE =
			CodeProviderHelpers.PATH_SEPARATOR + CodeProviderHelpers.FILENAME_CLEANTHAT_FOLDER
					+ CodeProviderHelpers.PATH_SEPARATOR
					+ "eclipse_formatter-stylesheet.xml";

	final ICodeProvider codeProvider;

	public JavaFormatterStepFactory(ICodeProvider codeProvider, SpotlessFormatterProperties formatterProperties) {
		super(codeProvider, formatterProperties);

		this.codeProvider = codeProvider;
	}

	@Override
	public Set<String> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	@Override
	public String licenseHeaderDelimiter() {
		return LICENSE_HEADER_DELIMITER;
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public FormatterStep makeStep(SpotlessStepProperties s, Provisioner provisioner) {
		String stepId = s.getId();
		switch (stepId) {
		case "removeUnusedImports": {
			return RemoveUnusedImportsStep.create(provisioner);
		}
		case "importOrder": {
			// https://stackoverflow.com/questions/34450900/how-to-sort-import-statements-in-eclipse-in-case-insensitive-order
			boolean wildcardsLast = s.getCustomProperty("wildcardsLast", Boolean.class);

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
			return ImportOrderStep.forJava().createFrom(wildcardsLast, ordersFile);
		}
		case "eclipse": {
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
					settingsFile = locateFile(stylesheetFile.toString());
				} catch (IOException e) {
					throw new UncheckedIOException("Issue processing eclipse.file: " + stylesheetFile, e);
				}
				eclipseConfig.setPreferences(Arrays.asList(settingsFile));
			}
			return eclipseConfig.build();
		}
		case "licenseHeader": {
			// com.diffplug.spotless.maven.generic.LicenseHeader
			String delimiter = s.getCustomProperty("delimiter", String.class);
			if (delimiter == null) {
				delimiter = licenseHeaderDelimiter();
			}
			if (delimiter == null) {
				throw new IllegalArgumentException("You need to specify 'delimiter'.");
			}
			String file = s.getCustomProperty(KEY_FILE, String.class);
			String content;
			if (file != null) {
				if (s.getCustomProperty("content", String.class) != null) {
					throw new IllegalArgumentException("Can not set both 'file' and 'content'");
				}

				byte[] fileBytes = PepperResourceHelper.loadAsBinary(CleanthatUrlLoader.loadUrl(codeProvider, file));
				content = new String(fileBytes, StandardCharsets.UTF_8);
			} else {
				content = s.getCustomProperty("content", String.class);
			}
			// Enable with next Spotless version
			// String skipLinesMatching = s.getCustomProperty("skipLinesMatching", String.class);

			YearMode yearMode = YearMode.PRESERVE;
			return LicenseHeaderStep.headerDelimiter(() -> content, delimiter)
					.withYearMode(yearMode)
					// .withSkipLinesMatching(skipLinesMatching)
					.build()
					.filterByFile(LicenseHeaderStep.unsupportedJvmFilesFilter());
		}
		default: {
			throw new IllegalArgumentException("Unknown Java step: " + stepId);
		}
		}
	}

	private File locateFile(String stylesheetFile) throws IOException {
		Resource resource = CleanthatUrlLoader.loadUrl(codeProvider, stylesheetFile);

		if (resource.isFile()) {
			return resource.getFile();
		}

		Path tmpFileAsPath = Files.createTempFile("cleanthat-spotless-eclipse-", ".xml");

		Files.copy(resource.getInputStream(), tmpFileAsPath, StandardCopyOption.REPLACE_EXISTING);
		File tmpFile = tmpFileAsPath.toFile();
		tmpFile.deleteOnExit();

		return tmpFile;
	}

	// This is useful to demonstrate all available configuration
	public static List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties removeUnusedImports = new SpotlessStepProperties();
		removeUnusedImports.setId("removeUnusedImports");

		SpotlessStepProperties importOrder = new SpotlessStepProperties();
		importOrder.setId("importOrder");
		importOrder.putProperty(KEY_FILE, "repository:/.cleanthat/java-importOrder.properties");

		SpotlessStepProperties eclipse = new SpotlessStepProperties();
		eclipse.setId("eclipse");
		eclipse.putProperty("version", EclipseJdtFormatterStep.defaultVersion());
		eclipse.putProperty(KEY_FILE, "repository:/.cleanthat/java-eclipse_stylesheet.xml");

		return ImmutableList.<SpotlessStepProperties>builder()
				.add(removeUnusedImports)
				.add(importOrder)
				.add(eclipse)
				.build();
	}

}
