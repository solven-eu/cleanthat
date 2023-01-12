package eu.solven.cleanthat.spotless;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.Provisioner;

import eu.solven.cleanthat.spotless.language.JavaFormatterStepFactory;
import eu.solven.cleanthat.spotless.mvn.ArtifactResolver;
import eu.solven.cleanthat.spotless.mvn.MavenProvisioner;

// see com.diffplug.spotless.maven.SpotlessApplyMojo
public class ExecuteSpotless {
	private static final class CleanthatJvmProvisioner implements Provisioner {
		@Override
		public Set<File> provisionWithTransitives(boolean withTransitives, Collection<String> mavenCoordinates) {
			LOGGER.error("TODO");
			return Set.of();
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteSpotless.class);

	final Formatter formatter;

	public ExecuteSpotless(Formatter formatter) {
		this.formatter = formatter;
	}

	// com.diffplug.gradle.spotless.SpotlessTask#buildFormatter
	public static Formatter makeFormatter(SpotlessProperties spotlessProperties, Provisioner provisioner) {
		Path tmpRoot;
		try {
			tmpRoot = Files.createTempDirectory("cleanthat-spotless-");
		} catch (IOException e) {
			throw new UncheckedIOException("Issue creating tmp rootDirectory", e);
		}

		// BEWARE may rely on formatterLineEndings.createPolicy(config.getFileLocator().getBaseDir(), filesToFormat)
		LineEnding.Policy lineEndingsPolicy = LineEnding.valueOf(spotlessProperties.getLineEnding()).createPolicy();

		// FormatExceptionPolicy.failOnlyOnError()
		FormatExceptionPolicy exceptionPolicy = new FormatExceptionPolicyStrict();

		List<FormatterStep> steps = buildSteps(spotlessProperties, provisioner);
		return Formatter.builder()
				.lineEndingsPolicy(lineEndingsPolicy)
				.encoding(Charset.forName(spotlessProperties.getEncoding()))
				.rootDir(tmpRoot)
				.steps(steps)
				.exceptionPolicy(exceptionPolicy)
				.build();
	}

	private static List<FormatterStep> buildSteps(SpotlessProperties spotlessProperties, Provisioner provisioner) {
		String language = spotlessProperties.getLanguage();

		AFormatterStepFactory stepFactory = makeFormatterStepFactory(language);

		// Provisioner provisionner = new CleanthatJvmProvisioner();
		Provisioner provisionner = makeProvisionner();

		return spotlessProperties.getSteps()
				.stream()
				.map(s -> stepFactory.makeStep(s, provisionner))
				.collect(Collectors.toList());
	}

	private static Provisioner makeProvisionner() {
		RepositorySystem repositorySystem = new DefaultRepositorySystem();
		RepositorySystemSession repositorySystemSession = new DefaultRepositorySystemSession();
		List<RemoteRepository> repositories = new ArrayList<>();
		Log log = new SystemStreamLog();
		Provisioner provisionner = MavenProvisioner
				.create(new ArtifactResolver(repositorySystem, repositorySystemSession, repositories, log));
		return provisionner;
	}

	private static AFormatterStepFactory makeFormatterStepFactory(String language) {
		switch (language) {
		case "java":
			return new JavaFormatterStepFactory();

		default:
			throw new IllegalArgumentException("Not managed language: " + language);
		}
	}

	/**
	 * 
	 * @param relativePath
	 *            the relativePath of current file, relative to the root (which is typically a Git repository root,
	 *            which may or may not reside on the FileSystem).
	 * @param rawBytes
	 * @return
	 */
	// com.diffplug.gradle.spotless.IdeHook#performHook
	// com.diffplug.spotless.maven.SpotlessApplyMojo#process
	public String doStuff(String relativePath, String rawBytes) {
		assert relativePath.startsWith(Pattern.quote("/"));

		Path root = formatter.getRootDir();
		File filePath = root.resolve("." + relativePath).toFile();

		try {
			PaddedCell.DirtyState dirty =
					PaddedCell.calculateDirtyState(formatter, filePath, rawBytes.getBytes(StandardCharsets.UTF_8));
			if (dirty.isClean()) {
				LOGGER.debug("This is already clean: {}", filePath);
				return rawBytes;
			} else if (dirty.didNotConverge()) {
				LOGGER.info("Spotless did not converge. {}",
						"Run 'spotlessDiagnose' for details https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md");
				return rawBytes;
			} else {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				dirty.writeCanonicalTo(baos);

				return new String(baos.toByteArray(), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to format file " + filePath, e);
		}
	}

	private Set<String> getIncludes(AFormatterStepFactory formatterFactory) {
		Set<String> configuredIncludes = formatterFactory.includes();
		Set<String> includes = configuredIncludes.isEmpty() ? formatterFactory.defaultIncludes() : configuredIncludes;
		if (includes.isEmpty()) {
			throw new IllegalArgumentException(
					"You must specify some files to include, such as '<includes><include>src/**/*.blah</include></includes>'");
		}
		return includes;
	}

	private Set<String> getExcludes(AFormatterStepFactory formatterFactory) {
		Set<String> configuredExcludes = formatterFactory.excludes();

		Set<String> excludes = new HashSet<>(FileUtils.getDefaultExcludesAsList());
		// excludes.add(withTrailingSeparator(buildDir.toString()));
		excludes.addAll(configuredExcludes);
		return excludes;
	}
}
