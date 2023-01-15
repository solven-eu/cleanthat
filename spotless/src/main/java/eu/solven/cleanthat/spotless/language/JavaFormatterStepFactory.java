package eu.solven.cleanthat.spotless.language;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import org.springframework.core.io.Resource;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.SpotlessProperties;
import eu.solven.cleanthat.spotless.SpotlessStepProperties;

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

	public JavaFormatterStepFactory(ICodeProvider codeProvider, SpotlessProperties spotlessProperties) {
		super(codeProvider,
				spotlessProperties.getIncludes().toArray(String[]::new),
				spotlessProperties.getExcludes().toArray(String[]::new));

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
		String stepName = s.getName();
		switch (stepName) {
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
			throw new IllegalArgumentException("Unknown Java step: " + stepName);
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
