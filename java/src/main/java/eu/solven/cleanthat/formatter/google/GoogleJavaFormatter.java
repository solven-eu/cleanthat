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
package eu.solven.cleanthat.formatter.google;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.github.ISourceCodeProperties;

/**
 * Google Java Formatter
 *
 * @author Benoit Lacelle
 */
public class GoogleJavaFormatter implements ISourceCodeFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleJavaFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;
	final GoogleJavaFormatterProperties processorConfig;

	public GoogleJavaFormatter(ISourceCodeProperties sourceCodeProperties,
			GoogleJavaFormatterProperties processorConfig) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.processorConfig = processorConfig;
	}

	@Override
	public String doFormat(String code, LineEnding eol) throws IOException {
		try {
			Formatter formatter = new Formatter();
			String output = formatter.formatSource(code);
			if (output.equals(code)) {
				LOGGER.debug("No change in the source");
			} else {
				LOGGER.debug("Some change in the source");
			}
			return output;
		} catch (FormatterException e) {
			// https://github.com/spring-io/spring-javaformat/blob/master/spring-javaformat-maven/spring-javaformat-maven-plugin/src/main/java/io/spring/format/maven/ApplyMojo.java
			throw new IllegalArgumentException("Unable to format code", e);
		}
	}
}
