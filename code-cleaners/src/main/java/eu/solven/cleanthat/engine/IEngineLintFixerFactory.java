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

import java.util.Map;
import java.util.Set;

import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.language.IEngineProperties;

/**
 * Knows how to format a String
 *
 * @author Benoit Lacelle
 */
public interface IEngineLintFixerFactory {
	String KEY_ENGINE = "engine";
	String KEY_PARAMETERS = "parameters";

	String getEngine();

	ILintFixer makeLintFixer(CleanthatSession cleanthatSession,
			IEngineProperties engineProperties,
			CleanthatStepProperties stepProperties);

	CleanthatEngineProperties makeDefaultProperties();

	Map<String, String> makeCustomDefaultFiles(CleanthatEngineProperties engineProperties);

	/**
	 * The typical file concerned by this {@link ILintFixer}
	 * 
	 * @return the {@link Set} of relevant include patterns.
	 */
	Set<String> getDefaultIncludes();
}
