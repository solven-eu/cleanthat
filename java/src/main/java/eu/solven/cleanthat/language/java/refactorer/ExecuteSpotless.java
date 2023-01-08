package eu.solven.cleanthat.language.java.refactorer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatExceptionPolicyStrict;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.ThrowingEx.Supplier;
import com.diffplug.spotless.generic.PipeStepPair;

// see com.diffplug.spotless.maven.SpotlessApplyMojo
public class ExecuteSpotless {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteSpotless.class);

	final Formatter formatter;

	public ExecuteSpotless(Formatter formatter) {
		this.formatter = formatter;
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
	
	// com.diffplug.spotless.maven.FormatterFactory#newFormatter
	public final Formatter newFormatter(Supplier<Iterable<File>> filesToFormat, FormatterConfig config) {
		Charset formatterEncoding = encoding(config);
		LineEnding formatterLineEndings = lineEndings(config);
		LineEnding.Policy formatterLineEndingPolicy = formatterLineEndings.createPolicy(config.getFileLocator().getBaseDir(), filesToFormat);

		FormatterStepConfig stepConfig = stepConfig(formatterEncoding, config);
		List<FormatterStepFactory> factories = gatherStepFactories(config.getGlobalStepFactories(), stepFactories);

		List<FormatterStep> formatterSteps = factories.stream()
				.filter(Objects::nonNull) // all unrecognized steps from XML config appear as nulls in the list
				.map(factory -> factory.newFormatterStep(stepConfig))
				.collect(Collectors.toCollection(() -> new ArrayList<FormatterStep>()));
		if (toggle != null) {
			PipeStepPair pair = toggle.createPair();
			formatterSteps.add(0, pair.in());
			formatterSteps.add(pair.out());
		}

		return Formatter.builder()
				.encoding(formatterEncoding)
				.lineEndingsPolicy(formatterLineEndingPolicy)
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.steps(formatterSteps)
				.rootDir(config.getFileLocator().getBaseDir().toPath())
				.build();
	}
	
	// com.diffplug.gradle.spotless.SpotlessTask#buildFormatter
	Formatter buildFormatter() {
		return Formatter.builder()
				.lineEndingsPolicy(lineEndingsPolicy.get())
				.encoding(Charset.forName(encoding))
				.rootDir(getProjectDir().get().getAsFile().toPath())
				.steps(steps.get())
				.exceptionPolicy(exceptionPolicy)
				.build();
	}
}
