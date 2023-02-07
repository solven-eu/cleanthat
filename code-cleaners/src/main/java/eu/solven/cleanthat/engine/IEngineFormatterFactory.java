/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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

import java.util.Set;

import eu.solven.cleanthat.language.IEngineProperties;

/**
 * Make {@link ICodeFormatterApplier} for different languages.
 * 
 * @author Benoit Lacelle
 *
 */
public interface IEngineFormatterFactory {
	IEngineLintFixerFactory makeLanguageFormatter(IEngineProperties languageProperties);

	/**
	 * 
	 * @param engine
	 * @return the default included patterns (glob or regex) for this engine
	 */
	Set<String> getDefaultIncludes(String engine);
}
