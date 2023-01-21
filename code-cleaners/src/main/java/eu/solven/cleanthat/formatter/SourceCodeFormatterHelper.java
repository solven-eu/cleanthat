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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ISkippable;
import eu.solven.cleanthat.engine.EnginePropertiesAndBuildProcessors;
import eu.solven.cleanthat.engine.ILanguageLintFixerFactory;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.pepper.collection.PepperMapHelper;

/**
 * Helps compiling CodeProcessors in the context of a repository
 * 
 * @author Benoit Lacelle
 *
 */
public class SourceCodeFormatterHelper {
	private final ObjectMapper objectMapper;

	public SourceCodeFormatterHelper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public EnginePropertiesAndBuildProcessors compile(IEngineProperties languageProperties,
			CleanthatSession cleanthatSession,
			ILanguageLintFixerFactory lintFixerFactory) {
		List<Map.Entry<IEngineProperties, ILintFixer>> processors =
				computeLintFixers(languageProperties, cleanthatSession, lintFixerFactory);

		return new EnginePropertiesAndBuildProcessors(processors);
	}

	/**
	 * 
	 * @param languageProperties
	 * @param cleanthatSession
	 *            necessary if some configuration is in the code itself
	 * @param lintFixerFactory
	 * @return
	 */
	public List<Map.Entry<IEngineProperties, ILintFixer>> computeLintFixers(IEngineProperties languageProperties,
			CleanthatSession cleanthatSession,
			ILanguageLintFixerFactory lintFixerFactory) {
		ConfigHelpers configHelpers = new ConfigHelpers(Collections.singleton(objectMapper));

		List<Map.Entry<IEngineProperties, ILintFixer>> processors =
				languageProperties.getSteps().stream().filter(rawProcessor -> {
					Optional<Boolean> optSkip =
							PepperMapHelper.<Boolean>getOptionalAs(rawProcessor, ISkippable.KEY_SKIP);

					if (optSkip.isEmpty()) {
						// By default, we do not skip
						return true;
					} else {
						Boolean skip = optSkip.get();

						// Execute processor if not skipped
						return !skip;
					}
				}).map(rawProcessor -> {
					IEngineProperties mergedLanguageProperties =
							configHelpers.mergeLanguageIntoProcessorProperties(languageProperties, rawProcessor);
					ILintFixer formatter =
							lintFixerFactory.makeLintFixer(rawProcessor, languageProperties, cleanthatSession);
					return Maps.immutableEntry(mergedLanguageProperties, formatter);
				}).collect(Collectors.toList());
		return processors;
	}
}
