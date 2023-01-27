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
package eu.solven.cleanthat.spotless;

import static java.util.Collections.emptySet;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.generic.LicenseHeaderStep.YearMode;
import com.google.common.collect.Sets;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import eu.solven.pepper.resource.PepperResourceHelper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Set;
import org.springframework.core.io.Resource;

/**
 * COmmon behavior to any Spotless engine steps factory
 * 
 * @author Benoit Lacelle
 *
 */
// see com.diffplug.spotless.maven.FormatterFactory
public abstract class AFormatterStepFactory {
	public static final String KEY_FILE = "file";

	private final SpotlessFormatterProperties spotlessProperties;

	final ICodeProvider codeProvider;

	@SuppressWarnings({ "PMD.UseVarargs", "PMD.ArrayIsStoredDirectly" })
	public AFormatterStepFactory(ICodeProvider codeProvider, SpotlessFormatterProperties spotlessProperties) {
		this.codeProvider = codeProvider;
		this.spotlessProperties = spotlessProperties;
	}

	public abstract Set<String> defaultIncludes();

	public abstract String licenseHeaderDelimiter();

	public final Set<String> getIncludes() {
		Collection<String> includes = spotlessProperties.getIncludes();
		if (includes == null) {
			return emptySet();
		} else {
			return Sets.newHashSet(includes);
		}
	}

	public final Set<String> getExcludes() {
		Collection<String> excludes = spotlessProperties.getExcludes();
		if (excludes == null) {
			return emptySet();
		} else {
			return Sets.newHashSet(excludes);
		}
	}

	public abstract FormatterStep makeStep(SpotlessStepProperties s, Provisioner provisioner);

	protected File locateFile(String stylesheetFile) throws IOException {
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

	protected FormatterStep makeLicenseHeader(SpotlessStepProperties s) {
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

}
