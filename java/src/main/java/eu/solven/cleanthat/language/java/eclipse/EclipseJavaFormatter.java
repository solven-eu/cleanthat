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
package eu.solven.cleanthat.language.java.eclipse;

import java.io.IOException;
import java.util.Map;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.formatter.LineEnding;

/**
 * Bridges to Eclipse formatting engine
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
// Example configurations:
// https://raw.githubusercontent.com/spring-io/spring-javaformat/master/.eclipse/eclipse-code-formatter.xml
// https://raw.githubusercontent.com/solven-eu/pepper/master/static/src/main/resources/eclipse/eclipse_java_code_formatter.xml
public class EclipseJavaFormatter implements IStyleEnforcer {
	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseJavaFormatter.class);

	public static final String ID = "eclipse_formatter";

	private final Map<String, String> defaultSettings;

	public EclipseJavaFormatter(EclipseJavaFormatterConfiguration configuration) {
		defaultSettings = configuration.getSettings();
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		// Make a new formatter to enable thread-safety
		CodeFormatter formatter = makeFormatter();

		TextEdit textEdit;
		try {
			textEdit = formatter.format(CodeFormatter.K_COMPILATION_UNIT
					| CodeFormatter.F_INCLUDE_COMMENTS, code, 0, code.length(), 0, ending.getChars());
			if (textEdit == null) {
				LOGGER.warn("Code cannot be formatted. Possible cause is unmatched source/target/compliance version.");
				return null;
			}
		} catch (RuntimeException e) {
			LOGGER.warn("Code cannot be formatted", e);
			return null;
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
