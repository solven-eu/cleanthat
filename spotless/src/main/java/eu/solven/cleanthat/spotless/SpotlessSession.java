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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.codehaus.plexus.util.MatchPatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.PaddedCell;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.PathAndContent;

/**
 * Trigger Spotless engine
 * 
 * @author Benoit Lacelle
 *
 */
// see com.diffplug.spotless.maven.SpotlessApplyMojo
public class SpotlessSession {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpotlessSession.class);

	final ImpactedFilesTracker filesTracker = new ImpactedFilesTracker();

	public ImpactedFilesTracker getFilesTracker() {
		return filesTracker;
	}

	public boolean acceptPath(EnrichedFormatter formatter, Path path) {
		var rawPath = path.toString();

		MatchPatterns includePatterns =
				MatchPatterns.from(withNormalizedFileSeparators(getIncludes(formatter.formatterStepFactory)));
		MatchPatterns excludePatterns =
				MatchPatterns.from(withNormalizedFileSeparators(getExcludes(formatter.formatterStepFactory)));

		if (!includePatterns.matches(rawPath, true)) {
			LOGGER.debug("Discarded by include: {}", path);
			return false;
		} else if (excludePatterns.matches(rawPath, true)) {
			LOGGER.debug("Discarded by exclude: {}", path);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 
	 * @param pathAndContent
	 *            the relativePath of current file, relative to the root (which is typically a Git repository root,
	 *            which may or may not reside on the FileSystem).
	 * @return
	 */
	// com.diffplug.gradle.spotless.IdeHook#performHook
	// com.diffplug.spotless.maven.SpotlessApplyMojo#process
	public String doStuff(EnrichedFormatter formatter, PathAndContent pathAndContent) {
		var path = pathAndContent.getPath();

		var rawPath = path.toString();

		MatchPatterns includePatterns =
				MatchPatterns.from(withNormalizedFileSeparators(getIncludes(formatter.formatterStepFactory)));
		MatchPatterns excludePatterns =
				MatchPatterns.from(withNormalizedFileSeparators(getExcludes(formatter.formatterStepFactory)));

		var rawBytes = pathAndContent.getContent();
		if (!includePatterns.matches(rawPath, true)) {
			return rawBytes;
		} else if (excludePatterns.matches(rawPath, true)) {
			return rawBytes;
		}

		File fakePathForSpotlessApi = getFakeFile(formatter.getFormatter().getRootDir(), path);
		if (!fakePathForSpotlessApi.isAbsolute()) {
			throw new IllegalArgumentException(
					"Spotless expects an absolute file, while it received: " + fakePathForSpotlessApi);
		}

		try {
			PaddedCell.DirtyState dirty = PaddedCell.calculateDirtyState(formatter.formatter,
					fakePathForSpotlessApi,
					rawBytes.getBytes(StandardCharsets.UTF_8));
			if (dirty.isClean()) {
				LOGGER.debug("This is already clean: {}", fakePathForSpotlessApi);
				filesTracker.checkedButAlreadyClean();
				return rawBytes;
			} else if (dirty.didNotConverge()) {
				LOGGER.info("Spotless did not converge. {}",
						"Run 'spotlessDiagnose' for details https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md");
				filesTracker.checkedButAlreadyClean();
				return rawBytes;
			} else {
				var baos = new ByteArrayOutputStream();

				dirty.writeCanonicalTo(baos);

				filesTracker.cleaned();
				return new String(baos.toByteArray(), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to format path=" + path + " with " + formatter, e);
		}
	}

	/**
	 * 
	 * @param root
	 *            the root as provided by a {@link ICodeProvider}
	 * @param relativeContentPath
	 *            a relative path, as a contentPath from an {@link ICodeProvider}
	 * @return a File for the default FileSystem (as required by Spotless)
	 */
	protected File getFakeFile(Path root, Path relativeContentPath) {
		// This check is more to demonstrate the kind of expectations over given path
		CleanthatPathHelpers.checkContentPath(relativeContentPath);
		var absoluteContentPath = root.resolve(relativeContentPath);

		// Spotless expects absolute pathes, as they will be compared with some root
		if (absoluteContentPath.getFileSystem().equals(FileSystems.getDefault())) {
			return new File(absoluteContentPath.toString());
		} else {
			var realFsFakeRoot = FileSystems.getDefault().getSeparator();
			// The fake root try to limit the risk of writing in improper locations
			String someFakeRootDirectory = "cleanthat_fake_root_for_spotless";
			var preventCollisions = UUID.randomUUID().toString();

			// Spotless requires a real File (instead of a Path)
			var fakeRootInRealFs =
					FileSystems.getDefault().getPath(realFsFakeRoot, someFakeRootDirectory, preventCollisions);

			// This will make sure we can not traverse alternative directories
			var asRelativePath = absoluteContentPath.getRoot().relativize(absoluteContentPath);
			// .toString() as FileSystem does not match
			var fakeContentInRealFs = CleanthatPathHelpers.resolveChild(fakeRootInRealFs, asRelativePath.toString());

			return fakeContentInRealFs.toFile();
		}
	}

	// com.diffplug.spotless.maven.AbstractSpotlessMojo#getIncludes
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

	// com.diffplug.spotless.maven.AbstractSpotlessMojo#getExcludes
	protected Set<String> getExcludes(AFormatterStepFactory formatterFactory) {
		Set<String> configuredExcludes = formatterFactory.getExcludes();

		Set<String> excludes = new HashSet<>();
		// excludes.addAll(FileUtils.getDefaultExcludesAsList());
		// excludes.add(withTrailingSeparator(buildDir.toString()));
		excludes.addAll(configuredExcludes);
		return excludes;
	}

	// com.diffplug.spotless.maven.AbstractSpotlessMojo#withNormalizedFileSeparators
	private Iterable<String> withNormalizedFileSeparators(Iterable<String> patterns) {
		return StreamSupport.stream(patterns.spliterator(), true)
				.map(pattern -> pattern.replace('/', File.separatorChar))
				.map(pattern -> pattern.replace('\\', File.separatorChar))
				.collect(Collectors.toSet());
	}

}
