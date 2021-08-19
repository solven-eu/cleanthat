package eu.solven.cleanthat.language.java.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.language.java.eclipse.checkstyle.XmlProfileWriter;

/**
 * This helps generating a proper Eclipse Stylesheet, based on the existing codebase: it will generate a stylesheet
 * minifying impacts over the codebase (supposing the codebase is well formatted)
 * 
 * @author Benoit Lacelle
 *
 */
// Convert from Checkstyle
// https://github.com/checkstyle/eclipse-cs/blob/master/net.sf.eclipsecs.core/src/net/sf/eclipsecs/core/jobs/TransformCheckstyleRulesJob.java
public class GenerateEclipseStylesheet {
	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEclipseStylesheet.class);

	protected GenerateEclipseStylesheet() {
		// hidden
	}

	public Path writeInTmp() throws IOException {
		DefaultCodeFormatterOptions defaultSettings = DefaultCodeFormatterOptions.getDefaultSettings();

		Map<String, String> defaultSettingsAsMap = defaultSettings.getMap();

		Path tmpFile = writeConfigurationToTmpPath(defaultSettingsAsMap);

		return tmpFile;
	}

	public Path writeConfigurationToTmpPath(Map<String, String> defaultSettingsAsMap) throws IOException {
		Path tmpFile = Files.createTempFile("cleanthat-eclipse-formatter-", ".xml");
		LOGGER.info("About to write Eclipse formatter configuration in {}", tmpFile);

		try (InputStream is = XmlProfileWriter.writeFormatterProfileToStream("cleanthat", defaultSettingsAsMap);
				OutputStream outputStream = Files.newOutputStream(tmpFile)) {
			ByteStreams.copy(is, outputStream);
		} catch (TransformerException | ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
		LOGGER.info("Done writing Eclipse formatter configuration in {}", tmpFile);
		return tmpFile;
	}
}
