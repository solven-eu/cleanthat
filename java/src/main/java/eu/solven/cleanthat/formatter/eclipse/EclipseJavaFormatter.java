/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.formatter.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.common.base.Strings;

import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.formatter.eclipse.revelc.ConfigReadException;
import eu.solven.cleanthat.formatter.eclipse.revelc.ConfigReader;
import eu.solven.cleanthat.github.CleanthatEclipsejavaFormatterProcessorProperties;
import eu.solven.cleanthat.github.CleanthatJavaProcessorProperties;
import eu.solven.cleanthat.github.ILanguageProperties;

/**
 * Bridges to Eclipse formatting engine
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
public class EclipseJavaFormatter implements ICodeProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseJavaFormatter.class);

	private static final String KEY_URL = "url";

	// private static final String KEY_JDK_VERSION = "jdk_version";
	// private static final String DEFAULT_JDK_VERSION = "1.8";
	private final CodeFormatter formatter;

	public EclipseJavaFormatter(ILanguageProperties languageProperties,
			CleanthatEclipsejavaFormatterProcessorProperties processorConfig) {
		Map<String, String> options = new LinkedHashMap<>();
		String javaConfigFile = processorConfig.getUrl();
		// Eclipse default
		if (Strings.isNullOrEmpty(javaConfigFile)) {
			LOGGER.info("There is no {}. Switching to default formatting", KEY_URL);
			// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/FormatterMojo.java#L689
			// { "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "9", "10", "11" }
			LOGGER.info("There is no {}. Switching to default formatting", KEY_URL);
			String jdkVersion = languageProperties.getLanguageVersion();
			// if (optJdkVersion.isEmpty()) {
			// LOGGER.warn("No value for {}. Defaulted to: {}", KEY_JDK_VERSION, DEFAULT_JDK_VERSION);
			// }
			// String jdkVersion = optJdkVersion.orElse();
			options.put(JavaCore.COMPILER_SOURCE, jdkVersion);
			options.put(JavaCore.COMPILER_COMPLIANCE, jdkVersion);
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, jdkVersion);
		} else {
			LOGGER.info("Loading Eclipse java formatting configuration from {}", javaConfigFile);
			try (InputStream is = new URL(javaConfigFile).openStream()) {
				try {
					options = new ConfigReader().read(is);
				} catch (SAXException | ConfigReadException e) {
					throw new RuntimeException("Issue parsing config: " + javaConfigFile, e);
				}
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("Invalid java.config_uri: + javaConfigFile", e);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		this.formatter = ToolFactory.createCodeFormatter(options, ToolFactory.M_FORMAT_EXISTING);
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		TextEdit te;
		try {
			te = this.formatter.format(CodeFormatter.K_COMPILATION_UNIT
					| CodeFormatter.F_INCLUDE_COMMENTS, code, 0, code.length(), 0, ending.getChars());
			if (te == null) {
				LOGGER.debug("Code cannot be formatted. Possible cause is unmatched source/target/compliance version.");
				return null;
			}
		} catch (IndexOutOfBoundsException e) {
			LOGGER.debug("Code cannot be formatted for text -->" + code + "<--", e);
			return null;
		}
		IDocument doc = new Document(code);
		try {
			te.apply(doc);
		} catch (MalformedTreeException | BadLocationException e) {
			throw new IllegalArgumentException(e);
		}
		String formattedCode = doc.get();
		if (code.equals(formattedCode)) {
			LOGGER.debug("Formatting was a no-op");
		}
		return formattedCode;
	}
}
