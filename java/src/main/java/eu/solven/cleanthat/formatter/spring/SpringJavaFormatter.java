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
package eu.solven.cleanthat.formatter.spring;

import java.io.IOException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.github.ISourceCodeProperties;
import io.spring.javaformat.formatter.Formatter;

/**
 * Spring Java formatter
 *
 * @author Benoit Lacelle
 */
public class SpringJavaFormatter implements ISourceCodeFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringJavaFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;
	final SpringJavaFormatterProperties processorConfig;

	public SpringJavaFormatter(ISourceCodeProperties sourceCodeProperties,
			SpringJavaFormatterProperties processorConfig) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.processorConfig = processorConfig;
	}

	@Override
	public String doFormat(String code, LineEnding eol) throws IOException {
		try {
			Formatter formatter = new Formatter();
			TextEdit edit = formatter.format(code);

			String output;
			if (edit.hasChildren() || edit.getLength() > 0) {
				LOGGER.debug("Some change in the source");

				IDocument document = new Document(code);
				edit.apply(document);
				output = document.get();
			} else {
				LOGGER.debug("No change in the source");
				output = code;
			}

			return output;
		} catch (MalformedTreeException | BadLocationException e) {
			throw new IllegalArgumentException("Unable to format code", e);
		}
	}
}
