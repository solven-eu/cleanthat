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
package eu.solven.cleanthat.spotless;

import static java.util.Collections.emptySet;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.generic.LicenseHeaderStep.YearMode;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import eu.solven.pepper.memory.IPepperMemoryConstants;
import eu.solven.pepper.resource.PepperResourceHelper;

/**
 * COmmon behavior to any Spotless engine steps factory
 * 
 * @author Benoit Lacelle
 *
 */
// see com.diffplug.spotless.maven.FormatterFactory
public abstract class AFormatterStepFactory {
	public static final String KEY_FILE = "file";
	public static final String KEY_TYPE = "type";

	private static final Cache<String, File> CONTENT_TO_FILE =
			CacheBuilder.newBuilder().maximumSize(IPepperMemoryConstants.KB).build();

	final AFormatterFactory formatterFactory;
	final SpotlessFormatterProperties spotlessProperties;

	final ICodeProvider codeProvider;

	@SuppressWarnings({ "PMD.UseVarargs", "PMD.ArrayIsStoredDirectly" })
	public AFormatterStepFactory(AFormatterFactory formatterFactory,
			ICodeProvider codeProvider,
			SpotlessFormatterProperties spotlessProperties) {
		this.formatterFactory = formatterFactory;
		this.codeProvider = codeProvider;
		this.spotlessProperties = spotlessProperties;
	}

	public Set<String> defaultIncludes() {
		return formatterFactory.defaultIncludes();
	}

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

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	public FormatterStep makeStep(SpotlessStepProperties stepProperties, Provisioner provisioner) {
		Optional<FormatterStep> commonStep = makeCommonStep(stepProperties, provisioner);

		return commonStep.orElseGet(() -> makeSpecializedStep(stepProperties, provisioner));
	}

	/**
	 * 
	 * @param stepProperties
	 * @param provisioner
	 * @return an {@link Optional} {@link FormatterStep} common to all {@link AFormatterStepFactory}
	 */
	protected Optional<FormatterStep> makeCommonStep(SpotlessStepProperties stepProperties, Provisioner provisioner) {
		String stepName = stepProperties.getId();
		SpotlessStepParametersProperties parameters = stepProperties.getParameters();

		switch (stepName) {
		case "licenseHeader": {
			return Optional.of(makeLicenseHeader(parameters));
		}
		case "wtpEclipse": {
			return Optional.of(makeWtpEclipse(parameters, provisioner));
		}
		default: {
			return Optional.empty();
		}
		}
	}

	public abstract FormatterStep makeSpecializedStep(SpotlessStepProperties s, Provisioner provisioner);

	protected File locateFile(String stylesheetFile) throws IOException {
		Resource resource = CleanthatUrlLoader.loadUrl(codeProvider, stylesheetFile);

		if (resource.isFile()) {
			return resource.getFile();
		}

		byte[] content = ByteStreams.toByteArray(resource.getInputStream());

		File locatedFile;
		try {
			locatedFile = CONTENT_TO_FILE.get(Base64.getEncoder().encodeToString(content), () -> {
				String fileExt = com.google.common.io.Files.getFileExtension(stylesheetFile);
				Path tmpFileAsPath = Files.createTempFile("cleanthat-spotless-", "." + fileExt);

				Files.copy(resource.getInputStream(), tmpFileAsPath, StandardCopyOption.REPLACE_EXISTING);
				File tmpFile = tmpFileAsPath.toFile();
				tmpFile.deleteOnExit();

				return tmpFile;
			});
		} catch (ExecutionException e) {
			throw new RuntimeException("Issue provisioning tmpFile for " + resource.getFilename(), e);
		}

		return locatedFile;
	}

	protected FormatterStep makeLicenseHeader(SpotlessStepParametersProperties parameters) {
		// com.diffplug.spotless.maven.generic.LicenseHeader
		String delimiter = parameters.getCustomProperty("delimiter", String.class);
		if (delimiter == null) {
			delimiter = licenseHeaderDelimiter();
		}
		if (delimiter == null) {
			throw new IllegalArgumentException("You need to specify 'delimiter'.");
		}
		String file = parameters.getCustomProperty(KEY_FILE, String.class);
		String content;
		if (file != null) {
			if (parameters.getCustomProperty("content", String.class) != null) {
				throw new IllegalArgumentException("Can not set both 'file' and 'content'");
			}

			byte[] fileBytes = PepperResourceHelper.loadAsBinary(CleanthatUrlLoader.loadUrl(codeProvider, file));
			content = new String(fileBytes, StandardCharsets.UTF_8);
		} else {
			content = parameters.getCustomProperty("content", String.class);
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

	protected FormatterStep makeWtpEclipse(SpotlessStepParametersProperties parameters, Provisioner provisioner) {
		EclipseWtpFormatterStep type =
				EclipseWtpFormatterStep.valueOf(parameters.getCustomProperty(KEY_TYPE, String.class));

		EclipseBasedStepBuilder eclipseConfig = type.createBuilder(provisioner);
		String version = parameters.getCustomProperty("version", String.class);
		if (Strings.isNullOrEmpty(version)) {
			version = EclipseWtpFormatterStep.defaultVersion();
		}
		eclipseConfig.setVersion(version);
		@SuppressWarnings("unchecked")
		Collection<String> files = parameters.getCustomProperty("files", Collection.class);
		if (null != files) {
			eclipseConfig.setPreferences(files.stream().map(file -> {
				try {
					return locateFile(file);
				} catch (IOException e) {
					throw new UncheckedIOException("Issue loading " + file, e);
				}
			}).collect(Collectors.toList()));
		}
		return eclipseConfig.build();
	}

}
