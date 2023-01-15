package eu.solven.cleanthat.spotless;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.Provisioner;

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
					"You must specify some files to include, such as 'includes:\r\n- 'src/**/*.blah''");
		}
		return includes;
	}

	private Set<String> getExcludes(AFormatterStepFactory formatterFactory) {
		Set<String> configuredExcludes = formatterFactory.excludes();

		Set<String> excludes = new HashSet<>();
		// excludes.addAll(FileUtils.getDefaultExcludesAsList());
		// excludes.add(withTrailingSeparator(buildDir.toString()));
		excludes.addAll(configuredExcludes);
		return excludes;
	}
}
