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
package io.cormoran.cleanthat.formatter.eclipse;

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
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.common.base.Strings;

import eu.solven.cleanthat.github.CleanThatRepositoryProperties;
import io.cormoran.cleanthat.formatter.LineEnding;
import io.cormoran.cleanthat.formatter.eclipse.revelc.ConfigReadException;
import io.cormoran.cleanthat.formatter.eclipse.revelc.ConfigReader;

// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
public class EclipseJavaFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseJavaFormatter.class);

	private CodeFormatter formatter;

	public EclipseJavaFormatter(CleanThatRepositoryProperties properties) {
		Map<String, String> options = new LinkedHashMap<>();

		String javaConfigFile = properties.getJavaConfigUrl();

		// Eclipse default
		if (Strings.isNullOrEmpty(javaConfigFile)) {
			// https://github.com/revelc/formatter-maven-plugin/blob/8d18b56855e682940e746caadc33e2a40a6b15b7/src/main/java/net/revelc/code/formatter/FormatterMojo.java#L689
			options.put(JavaCore.COMPILER_SOURCE, "1.8");
			options.put(JavaCore.COMPILER_COMPLIANCE, "1.8");
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.8");
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

	public String doFormat(String code, LineEnding ending) throws IOException, BadLocationException {
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
		te.apply(doc);
		String formattedCode = doc.get();

		if (code.equals(formattedCode)) {
			return null;
		}
		return formattedCode;
	}

}