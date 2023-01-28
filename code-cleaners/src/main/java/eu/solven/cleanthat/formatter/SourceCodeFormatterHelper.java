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
package eu.solven.cleanthat.formatter;

import com.google.common.collect.Maps;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.engine.EnginePropertiesAndBuildProcessors;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.language.IEngineProperties;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helps compiling CodeProcessors in the context of a repository
 * 
 * @author Benoit Lacelle
 *
 */
public class SourceCodeFormatterHelper {

	public EnginePropertiesAndBuildProcessors compile(IEngineProperties engineProperties,
			CleanthatSession cleanthatSession,
			IEngineLintFixerFactory lintFixerFactory) {
		List<Map.Entry<IEngineProperties, ILintFixer>> processors =
				computeLintFixers(engineProperties, cleanthatSession, lintFixerFactory);

		return new EnginePropertiesAndBuildProcessors(processors);
	}

	/**
	 * 
	 * @param engineProperties
	 * @param cleanthatSession
	 *            necessary if some configuration is in the code itself
	 * @param lintFixerFactory
	 * @return
	 */
	public List<Map.Entry<IEngineProperties, ILintFixer>> computeLintFixers(IEngineProperties engineProperties,
			CleanthatSession cleanthatSession,
			IEngineLintFixerFactory lintFixerFactory) {
		List<Map.Entry<IEngineProperties, ILintFixer>> processors = engineProperties.getSteps()
				.stream()
				.filter(Predicate.not(CleanthatStepProperties::isSkip))
				.map(step -> {
					ILintFixer formatter = lintFixerFactory.makeLintFixer(step, engineProperties, cleanthatSession);
					return Maps.immutableEntry(engineProperties, formatter);
				})
				.collect(Collectors.toList());
		return processors;
	}
}
