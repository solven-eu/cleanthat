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
import com.google.common.collect.ImmutableSet;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;
import org.springframework.core.io.Resource;

/**
 * Configure Spotless engine for '.java' files
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaFormatterStepFactory extends AFormatterStepFactory {

	// CleanThat will call spotless from the root directory: process any Java file from there
	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.of("**/*.java");
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	final ICodeProvider codeProvider;

	@Deprecated
	public JavaFormatterStepFactory(ICodeProvider codeProvider, String[] includes, String[] excludes) {
		super(codeProvider, includes, excludes);

		this.codeProvider = codeProvider;
	}

	public JavaFormatterStepFactory(ICodeProvider codeProvider, SpotlessFormatterProperties formatterProperties) {
		super(codeProvider,
				formatterProperties.getIncludes().toArray(String[]::new),
				formatterProperties.getExcludes().toArray(String[]::new));

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

	@Override
	public FormatterStep makeStep(SpotlessStepProperties s, Provisioner provisioner) {
		String stepId = s.getId();
		switch (stepId) {
		case "eclipse": {
			EclipseBasedStepBuilder eclipseConfig = EclipseJdtFormatterStep.createBuilder(provisioner);

			Object eclipseVersion = s.getCustomProperty("version");
			eclipseConfig.setVersion(
					eclipseVersion == null ? EclipseJdtFormatterStep.defaultVersion() : eclipseVersion.toString());

			Object stylesheetFile = s.getCustomProperty("file");
			if (stylesheetFile instanceof String) {
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

		File tmpFile = Files.createTempFile("cleanthat-spotless-eclipse-", ".xml").toFile();
		tmpFile.deleteOnExit();

		return tmpFile;
	}

}
