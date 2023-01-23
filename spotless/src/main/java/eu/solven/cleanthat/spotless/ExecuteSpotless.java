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

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trigger effectiviely Spotless engine
 * 
 * @author Benoit Lacelle
 *
 */
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

		// Path root = formatter.getRootDir();
		File filePath = new File("");
		// root.resolve("." + relativePath).toFile();

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

	protected Set<String> getIncludes(AFormatterStepFactory formatterFactory) {
		Set<String> configuredIncludes = formatterFactory.getIncludes();
		Set<String> includes;
		if (configuredIncludes.isEmpty()) {
			includes = formatterFactory.defaultIncludes();
		} else {
			includes = configuredIncludes;
		}
		if (includes.isEmpty()) {
			throw new IllegalArgumentException(
					"You must specify some files to include, such as 'includes:\r\n- 'src/**/*.blah''");
		}
		return includes;
	}

	protected Set<String> getExcludes(AFormatterStepFactory formatterFactory) {
		Set<String> configuredExcludes = formatterFactory.getExcludes();

		Set<String> excludes = new HashSet<>();
		// excludes.addAll(FileUtils.getDefaultExcludesAsList());
		// excludes.add(withTrailingSeparator(buildDir.toString()));
		excludes.addAll(configuredExcludes);
		return excludes;
	}
}
