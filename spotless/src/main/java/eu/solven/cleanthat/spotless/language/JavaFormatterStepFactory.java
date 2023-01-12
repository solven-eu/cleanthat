package eu.solven.cleanthat.spotless.language;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.SpotlessStepProperties;

public class JavaFormatterStepFactory extends AFormatterStepFactory {

	// CleanThat will call spotless from the root directory: process any Java file from there
	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.of("**/*.java");
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	public JavaFormatterStepFactory(ICodeProvider codeProvider, String[] includes, String[] excludes) {
		super(codeProvider, includes, excludes);
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
		String stepName = s.getName();
		switch (stepName) {
		case "eclipse": {
			EclipseBasedStepBuilder eclipseConfig = EclipseJdtFormatterStep.createBuilder(provisioner);

			Object eclipseVersion = s.getCustomProperty("version");
			eclipseConfig.setVersion(
					eclipseVersion == null ? EclipseJdtFormatterStep.defaultVersion() : eclipseVersion.toString());

			Object stylesheetFile = s.getCustomProperty("file");
			if (null != stylesheetFile) {
				File settingsFile = locateFile(stylesheetFile);
				eclipseConfig.setPreferences(Arrays.asList(settingsFile));
			}
			return eclipseConfig.build();
		}
		default: {
			throw new IllegalArgumentException("Unknown Java step: " + stepName);
		}
		}
	}

	private File locateFile(Object stylesheetFile) {
		// File, or in dependency?
		// Cleanthat: File in code, or in URL
		throw new IllegalArgumentException("TODO");
	}

}
