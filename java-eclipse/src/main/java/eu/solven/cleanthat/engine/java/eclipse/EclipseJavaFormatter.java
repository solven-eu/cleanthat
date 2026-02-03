/*
 * Copyright 2023-2026 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.engine.java.eclipse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.pepper.logging.PepperLogHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * Bridges to Eclipse formatting engine
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
// Example configurations:
// https://raw.githubusercontent.com/spring-io/spring-javaformat/master/.eclipse/eclipse-code-formatter.xml
// https://raw.githubusercontent.com/solven-eu/pepper/master/static/src/main/resources/eclipse/eclipse_java_code_formatter.xml
@Slf4j
public class EclipseJavaFormatter implements ILintFixerWithId {

	public static final String ID = "eclipse_formatter";

	private final Map<String, String> defaultSettings;

	// For statistics purposes
	private static final AtomicInteger NB_FORMATTED = new AtomicInteger();
	private static final AtomicLong TIME_FORMATTING = new AtomicLong();

	public EclipseJavaFormatter(EclipseJavaFormatterConfiguration configuration) {
		defaultSettings = configuration.getSettings();
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String doFormat(String code) throws IOException {
		// Make a new formatter to enable thread-safety
		CodeFormatter formatter = makeFormatter();

		var start = System.currentTimeMillis();
		TextEdit textEdit;
		try {
			var eolChars = LineEnding.getOrGuess(LineEnding.NATIVE, () -> code);
			textEdit = formatter.format(CodeFormatter.K_COMPILATION_UNIT
					| CodeFormatter.F_INCLUDE_COMMENTS, code, 0, code.length(), 0, eolChars);
			if (textEdit == null) {
				LOGGER.warn("Code cannot be formatted. Possible cause is unmatched source/target/compliance version.");
				return null;
			}
		} catch (RuntimeException e) {
			LOGGER.warn("Code cannot be formatted", e);
			return null;
		} catch (AssertionError e) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=543646
			LOGGER.warn("Bug in Eclipse Formatter?", e);
			return null;
		} finally {
			var time = System.currentTimeMillis() - start;
			var totalDuration = TIME_FORMATTING.addAndGet(time);
			var totalFormats = NB_FORMATTED.incrementAndGet();
			if (Integer.bitCount(totalFormats) == 1) {
				LOGGER.info("Total Eclipse Formats: {}. MeanTime: {}",
						PepperLogHelper.humanBytes(totalFormats),
						PepperLogHelper.humanDuration(totalDuration / totalFormats));
			}
		}
		IDocument doc = new Document(code);
		try {
			textEdit.apply(doc);
		} catch (MalformedTreeException | BadLocationException e) {
			throw new IllegalArgumentException(e);
		}
		String formattedCode = doc.get();
		if (formattedCode == null) {
			throw new IllegalStateException("This happened while accessing concurrently a single CodeFormatter");
		} else if (code.equals(formattedCode)) {
			LOGGER.debug("Formatting was a no-op");
		}
		return formattedCode;
	}

	private CodeFormatter makeFormatter() {
		return ToolFactory.createCodeFormatter(defaultSettings, ToolFactory.M_FORMAT_EXISTING);
	}
}
