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
package eu.solven.cleanthat.language.java.google.intellij;

import java.io.IOException;

import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;

/**
 * IntelliJ Java Formatter
 *
 * @author Benoit Lacelle
 */
public class IntelliJJavaFormatter implements ISourceCodeFormatter {
	final ISourceCodeProperties sourceCodeProperties;
	final IntellijJavaFormatterProperties processorConfig;

	public IntelliJJavaFormatter(ISourceCodeProperties sourceCodeProperties,
			IntellijJavaFormatterProperties processorConfig) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.processorConfig = processorConfig;
	}

	@Override
	public String doFormat(String code, LineEnding eol) throws IOException {
		// https://www.jetbrains.com/help/idea/command-line-formatter.html
		// https://stackoverflow.com/questions/34197142/run-intellij-code-formatter-from-the-command-line
		throw new UnsupportedOperationException("TODO");
	}
}
