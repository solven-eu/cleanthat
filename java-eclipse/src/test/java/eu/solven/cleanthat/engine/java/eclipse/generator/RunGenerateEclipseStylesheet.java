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
package eu.solven.cleanthat.engine.java.eclipse.generator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.github.difflib.patch.PatchFailedException;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.engine.java.eclipse.EclipseJavaFormatter;
import eu.solven.cleanthat.engine.java.eclipse.EclipseJavaFormatterConfiguration;
import eu.solven.cleanthat.engine.java.eclipse.checkstyle.XmlProfileWriter;

/**
 * Execute the procedure to generate a minimizing Eclipse Formatter configuration
 *
 * @author Benoit Lacelle
 */
public class RunGenerateEclipseStylesheet {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunGenerateEclipseStylesheet.class);

	protected RunGenerateEclipseStylesheet() {
		// hidden
	}

	public static void main(String[] args)
			throws TransformerException, ParserConfigurationException, IOException, PatchFailedException {
		// Path writtenPath = stylesheetGenerator.writeInTmp();
		var rootForFiles = Paths.get("/Users/blacelle/workspace2/RoaringBitmap");
		// TODO We should exclude files matching .gitignore (e.g. everything in target folders)
		var fileMatcher = Pattern.compile(".*/src/main/java/.*\\.java");

		EclipseStylesheetGenerator stylesheetGenerator = new EclipseStylesheetGenerator();
		Map<Path, String> pathToFile = stylesheetGenerator.loadFilesContent(rootForFiles, fileMatcher);
		{
			Map<String, String> bestOptions =
					stylesheetGenerator.generateSettings(OffsetDateTime.now().plusHours(1), pathToFile);

			logDiffWithPepper(stylesheetGenerator, pathToFile, bestOptions);

			writeConfigurationToTmpPath(bestOptions);
		}
	}

	@Deprecated(since = "For debug purposes")
	private static void logDiffWithPepper(EclipseStylesheetGenerator stylesheetGenerator,
			Map<Path, String> pathToFile,
			Map<String, String> selectedOptions) {
		Map<String, String> pepperConvention;
		{
			pepperConvention = EclipseJavaFormatterConfiguration
					.loadResource(new ClassPathResource("/eclipse/pepper-eclipse-code-formatter.xml"))
					.getSettings();
		}

		LOGGER.info("Difference with Pepper:");
		MapDifference<String, String> diff = Maps.difference(pepperConvention, selectedOptions);
		diff.entriesDiffering().forEach((k, v) -> LOGGER.info("{} -> {}", k, v));
		EclipseJavaFormatterConfiguration config = new EclipseJavaFormatterConfiguration(pepperConvention);
		EclipseJavaFormatter formatter = new EclipseJavaFormatter(config);
		long pepperDiffScoreDiff =
				stylesheetGenerator.getCodeDiffHelper().computeDiffScore(formatter, pathToFile.values());
		LOGGER.info("Pepper diff: {}", pepperDiffScoreDiff);
	}

	@Deprecated
	public static Path writeInTmp() throws IOException {
		DefaultCodeFormatterOptions defaultSettings = DefaultCodeFormatterOptions.getDefaultSettings();

		Map<String, String> defaultSettingsAsMap = defaultSettings.getMap();

		var tmpFile = writeConfigurationToTmpPath(defaultSettingsAsMap);

		return tmpFile;
	}

	public static Path writeConfigurationToTmpPath(Map<String, String> settings) throws IOException {
		var tmpFile = Files.createTempFile("cleanthat-eclipse-formatter-", ".xml");
		LOGGER.info("About to write Eclipse formatter configuration in {}", tmpFile);

		try (InputStream is = XmlProfileWriter.writeFormatterProfileToStream("cleanthat", settings);
				var outputStream = Files.newOutputStream(tmpFile)) {
			ByteStreams.copy(is, outputStream);
		} catch (TransformerException | ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
		LOGGER.info("Done writing Eclipse formatter configuration in {}", tmpFile);
		return tmpFile;
	}
}
