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

import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.github.CleanthatEclipsejavaFormatterProcessorProperties;
import eu.solven.cleanthat.github.ILanguageProperties;

/**
 * Spring Java formatter
 *
 * @author Benoit Lacelle
 */
public class SpringJavaFormatter implements ISourceCodeFormatter {
	public SpringJavaFormatter(ILanguageProperties languageProperties,
			CleanthatEclipsejavaFormatterProcessorProperties processorConfig) {
		throw new IllegalStateException("TODO");
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		throw new IllegalStateException("TODO");
	}
}
