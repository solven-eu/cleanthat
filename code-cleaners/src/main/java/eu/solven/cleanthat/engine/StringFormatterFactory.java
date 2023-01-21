/*
 * Copyright 2023 Solven
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
package eu.solven.cleanthat.engine;

import eu.solven.cleanthat.language.IEngineProperties;
import java.util.Map;

/**
 * Default implementation for {@link ILanguageFormatterFactory}
 * 
 * @author Benoit Lacelle
 *
 */
public class StringFormatterFactory implements ILanguageFormatterFactory {

	final Map<String, ILanguageLintFixerFactory> languageToFormatter;

	public StringFormatterFactory(Map<String, ILanguageLintFixerFactory> languageToFormatter) {
		this.languageToFormatter = languageToFormatter;
	}

	@Override
	public ILanguageLintFixerFactory makeLanguageFormatter(IEngineProperties languageProperties) {
		String language = languageProperties.getEngine();
		ILanguageLintFixerFactory formatter = languageToFormatter.get(language);

		if (formatter == null) {
			throw new IllegalArgumentException(
					"There is no formatter for language=" + language + " languages=" + languageToFormatter.keySet());
		}

		return formatter;
	}

}
