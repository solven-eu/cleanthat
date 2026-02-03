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
package eu.solven.cleanthat.spotless;

import static java.util.Collections.emptySet;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep;
import com.diffplug.spotless.generic.EndWithNewlineStep;
import com.diffplug.spotless.generic.IndentStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.generic.LicenseHeaderStep.YearMode;
import com.diffplug.spotless.generic.PipeStepPair;
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep;
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
public abstract class AFormatterStepFactory implements IFormatterStepConstants {
	public static final String ID_TOGGLE_OFF_ON = "toggleOffOn";

	public static final String KEY_CONTENT = "content";

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

	/**
	 * 
	 * @param stepProperties
	 * @param provisioner
	 * @return a {@link Consumer} of {@link List} of {@link FormatterStep} describing how current
	 *         {@link SpotlessStepProperties} should impact such a {@link List}.
	 */
	public Consumer<List<FormatterStep>> makeStep(SpotlessStepProperties stepProperties, Provisioner provisioner) {
		Optional<Consumer<List<FormatterStep>>> commonStep = makeCommonStep(stepProperties, provisioner);

		return commonStep.orElseGet(() -> {
			FormatterStep formatterStep = makeSpecializedStep(stepProperties, provisioner);

			return l -> l.add(formatterStep);
		});
	}

	/**
	 * This returns a {@link Optional} as unknown steps may be language-specific steps. This returns a {@link Consumer}
	 * as some steps need specific position in the {@link List} of steps
	 * 
	 * @param stepProperties
	 * @param provisioner
	 * @return an {@link Optional} {@link FormatterStep} common to all {@link AFormatterStepFactory}
	 */
	protected Optional<Consumer<List<FormatterStep>>> makeCommonStep(SpotlessStepProperties stepProperties,
			Provisioner provisioner) {
		String stepName = stepProperties.getId();
		SpotlessStepParametersProperties parameters = stepProperties.getParameters();

		switch (stepName) {
		case ID_TOGGLE_OFF_ON: {
			// see ToggleOffOn
			PipeStepPair pair = PipeStepPair.named(PipeStepPair.defaultToggleName())
					.openClose(PipeStepPair.defaultToggleOff(), PipeStepPair.defaultToggleOn())
					.buildPair();

			return Optional.of(l -> {
				// Open the toggleOffOn feature as first step
				l.add(0, pair.in());
				l.add(pair.out());
			});
		}
		case "trimTrailingWhitespace": {
			return Optional.of(l -> l.add(TrimTrailingWhitespaceStep.create()));
		}
		case "endWithNewline": {
			return Optional.of(l -> l.add(EndWithNewlineStep.create()));
		}
		case "indent": {
			Integer spacesPerTab = parameters.getCustomProperty("spacesPerTab", Integer.class);
			if (spacesPerTab == null) {
				spacesPerTab = IndentStep.defaultNumSpacesPerTab();
			}

			Boolean spaces = parameters.getCustomProperty("spaces", Boolean.class);
			if (spaces == null) {
				spaces = false;
			}
			Boolean tabs = parameters.getCustomProperty("tabs", Boolean.class);
			if (tabs == null) {
				tabs = false;
			}

			FormatterStep indentStep;
			if (spaces ^ tabs) {
				if (spaces) {
					indentStep = IndentStep.create(IndentStep.Type.SPACE, spacesPerTab);
				} else {
					indentStep = IndentStep.create(IndentStep.Type.TAB, spacesPerTab);
				}
			} else {
				throw new IllegalArgumentException("Must specify exactly one of 'spaces: true' or 'tabs: true'.");
			}

			return Optional.of(l -> l.add(indentStep));
		}
		case "licenseHeader": {
			return Optional.of(l -> l.add(makeLicenseHeader(parameters)));
		}
		case "eclipseWtp": {
			return Optional.of(l -> l.add(makeEclipseWtp(parameters, provisioner)));
		}

		default: {
			return Optional.empty();
		}
		}
	}

	public abstract FormatterStep makeSpecializedStep(SpotlessStepProperties s, Provisioner provisioner);

	protected File locateFile(String stylesheetFile) throws IOException {
		var resource = CleanthatUrlLoader.loadUrl(codeProvider, stylesheetFile);

		if (resource.isFile()) {
			return resource.getFile();
		}

		var content = ByteStreams.toByteArray(resource.getInputStream());

		File locatedFile;
		try {
			locatedFile = CONTENT_TO_FILE.get(Base64.getEncoder().encodeToString(content), () -> {
				var fileExt = com.google.common.io.Files.getFileExtension(stylesheetFile);
				var tmpFileAsPath = Files.createTempFile("cleanthat-spotless-", "." + fileExt);

				Files.copy(resource.getInputStream(), tmpFileAsPath, StandardCopyOption.REPLACE_EXISTING);
				var tmpFile = tmpFileAsPath.toFile();
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
			if (parameters.getCustomProperty(KEY_CONTENT, String.class) != null) {
				throw new IllegalArgumentException("Can not set both 'file' and 'content'");
			}

			var fileBytes = PepperResourceHelper.loadAsBinary(CleanthatUrlLoader.loadUrl(codeProvider, file));
			content = new String(fileBytes, StandardCharsets.UTF_8);
		} else {
			content = parameters.getCustomProperty(KEY_CONTENT, String.class);
		}
		// Enable with next Spotless version
		// String skipLinesMatching = s.getCustomProperty("skipLinesMatching", String.class);

		// While the default mode in the step is PRESERVE
		// https://github.com/diffplug/spotless/blob/main/lib/src/main/java/com/diffplug/spotless/generic/LicenseHeaderStep.java#L61
		// mvn-plugin switches to UPDATE_TO_TODAY if there is a ratchetFrom
		// https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/generic/LicenseHeader.java#L55
		// As most of cleanthat work is done in comparison with some base branch, we are implicitly with a ratchetFrom
		// parameter
		// Hence, `UPDATE_TO_TODAY` seems the most relevant option
		// TODO Add an option through configuration?
		var yearMode = YearMode.UPDATE_TO_TODAY;

		return LicenseHeaderStep.headerDelimiter(() -> content, delimiter)
				.withYearMode(yearMode)
				// .withSkipLinesMatching(skipLinesMatching)
				.build()
				.filterByFile(LicenseHeaderStep.unsupportedJvmFilesFilter());
	}

	protected FormatterStep makeEclipseWtp(SpotlessStepParametersProperties parameters, Provisioner provisioner) {
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
