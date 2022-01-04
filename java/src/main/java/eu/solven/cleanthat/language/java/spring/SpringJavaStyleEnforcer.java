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
package eu.solven.cleanthat.language.java.spring;

import java.io.IOException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import io.spring.javaformat.formatter.Formatter;

/**
 * Spring Java {@link IStyleEnforcer}
 *
 * @author Benoit Lacelle
 */
// https://github.com/spring-io/spring-javaformat
public class SpringJavaStyleEnforcer implements IStyleEnforcer, ILintFixerWithId {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringJavaStyleEnforcer.class);

	final ISourceCodeProperties sourceCodeProperties;
	final SpringJavaFormatterProperties processorConfig;

	public SpringJavaStyleEnforcer(ISourceCodeProperties sourceCodeProperties,
			SpringJavaFormatterProperties processorConfig) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.processorConfig = processorConfig;
	}

	@Override
	public String getId() {
		return "spring_formatter";
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
