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

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.pojo.EngineProperties;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.language.IEngineProperties;

import java.util.Map;
import java.util.Set;

/**
 * Knows how to format a String
 *
 * @author Benoit Lacelle
 */
public interface ILanguageLintFixerFactory {
	String KEY_ENGINE = "engine";
	String KEY_PARAMETERS = "parameters";

	String getEngine();

	/**
	 * The typical file extensions concerned by this LintFixer
	 * 
	 * @return the {@link Set} of relevant file extensions.
	 */
	Set<String> getFileExtentions();

	ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			IEngineProperties languageProperties,
			ICodeProvider codeProvider);

	EngineProperties makeDefaultProperties();
}
