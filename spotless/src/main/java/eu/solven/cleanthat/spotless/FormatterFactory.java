package eu.solven.cleanthat.spotless;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

import com.diffplug.spotless.FormatExceptionPolicy;
import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.language.JavaFormatterStepFactory;
import eu.solven.cleanthat.spotless.mvn.ArtifactResolver;
import eu.solven.cleanthat.spotless.mvn.MavenProvisioner;

public class FormatterFactory {
	final ICodeProvider codeProvider;

	public FormatterFactory(ICodeProvider codeProvider) {
		this.codeProvider = codeProvider;
	}

	// Provisioner provisionner = new CleanthatJvmProvisioner();
	public static Provisioner makeProvisionner() {
		RepositorySystem repositorySystem = new DefaultRepositorySystem();
		RepositorySystemSession repositorySystemSession = new DefaultRepositorySystemSession();
		List<RemoteRepository> repositories = new ArrayList<>();
		Log log = new SystemStreamLog();
		Provisioner provisionner = MavenProvisioner
				.create(new ArtifactResolver(repositorySystem, repositorySystemSession, repositories, log));
		return provisionner;
	}

	private AFormatterStepFactory makeFormatterStepFactory(SpotlessProperties spotlessProperties) {
		String language = spotlessProperties.getLanguage();
		switch (language) {
		case "java":
			return new JavaFormatterStepFactory(codeProvider, spotlessProperties);

		default:
			throw new IllegalArgumentException("Not managed language: " + language);
		}
	}

	// com.diffplug.gradle.spotless.SpotlessTask#buildFormatter
	public Formatter makeFormatter(SpotlessProperties spotlessProperties, Provisioner provisioner) {
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

	private List<FormatterStep> buildSteps(SpotlessProperties spotlessProperties, Provisioner provisioner) {
		AFormatterStepFactory stepFactory = makeFormatterStepFactory(spotlessProperties);

		return spotlessProperties.getSteps()
				.stream()
				.map(s -> stepFactory.makeStep(s, provisioner))
				.collect(Collectors.toList());
	}
}
