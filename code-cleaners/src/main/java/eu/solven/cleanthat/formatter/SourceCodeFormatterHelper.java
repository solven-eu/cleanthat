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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ISkippable;
import eu.solven.cleanthat.engine.EnginePropertiesAndBuildProcessors;
import eu.solven.cleanthat.engine.ILanguageLintFixerFactory;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.pepper.collection.PepperMapHelper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps compiling CodeProcessors in the context of a repository
 * 
 * @author Benoit Lacelle
 *
 */
public class SourceCodeFormatterHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceCodeFormatterHelper.class);

	private final AtomicBoolean reportedLackOfStyleEnforcer = new AtomicBoolean();

	private final ObjectMapper objectMapper;

	public SourceCodeFormatterHelper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public EnginePropertiesAndBuildProcessors compile(IEngineProperties languageProperties,
			ICodeProvider codeProvider,
			ILanguageLintFixerFactory lintFixerFactory) {
		List<Map.Entry<IEngineProperties, ILintFixer>> processors =
				computeLintFixers(languageProperties, codeProvider, lintFixerFactory);

		List<IStyleEnforcer> codeStyleFixer = processors.stream()
				.map(e -> e.getValue())
				.filter(lf -> lf instanceof IStyleEnforcer)
				.map(lf -> (IStyleEnforcer) lf)
				.collect(Collectors.toList());

		String language = languageProperties.getEngine();

		// This is specific for java for the moment, and more precisely to RulesJavaMutator as AST manipulators has high
		// probability to change massively the style
		if (codeStyleFixer.isEmpty() && "java".equals(language)) {
			if (reportedLackOfStyleEnforcer.getAndSet(true)) {
				// Already reported
				LOGGER.debug("It is certainly unsafe not to have a single {} for language={}",
						IStyleEnforcer.class.getSimpleName(),
						language);
			} else {
				// TODO this is true only due to specific formatters (e.g. RulesJavaMutator)
				LOGGER.warn("It is certainly unsafe not to have a single {} for language={}",
						IStyleEnforcer.class.getSimpleName(),
						language);
			}
		} else if (!codeStyleFixer.isEmpty()) {
			int nbCodeStyleFormatter = codeStyleFixer.size();
			if (nbCodeStyleFormatter >= 2) {
				LOGGER.warn("It is unsual to have multiple={} {}",
						nbCodeStyleFormatter,
						IStyleEnforcer.class.getSimpleName());
			}

			IStyleEnforcer firstCodeStyleFormatter = codeStyleFixer.get(0);

			processors.stream()
					.map(e -> e.getValue())
					.filter(Predicates.instanceOf(ILintFixerHelpedByCodeStyleFixer.class))
					.map(lf -> (ILintFixerHelpedByCodeStyleFixer) lf)
					.forEach(lf -> {
						lf.registerCodeStyleFixer(firstCodeStyleFormatter);
					});
		}

		return new EnginePropertiesAndBuildProcessors(processors);
	}

	/**
	 * 
	 * @param languageProperties
	 * @param codeProvider
	 *            necessary if some configuration is in the code itself
	 * @param lintFixerFactory
	 * @return
	 */
	public List<Map.Entry<IEngineProperties, ILintFixer>> computeLintFixers(IEngineProperties languageProperties,
			ICodeProvider codeProvider,
			ILanguageLintFixerFactory lintFixerFactory) {
		ConfigHelpers configHelpers = new ConfigHelpers(Collections.singleton(objectMapper));

		List<Map.Entry<IEngineProperties, ILintFixer>> processors =
				languageProperties.getProcessors().stream().filter(rawProcessor -> {
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
							lintFixerFactory.makeLintFixer(rawProcessor, languageProperties, codeProvider);
					return Maps.immutableEntry(mergedLanguageProperties, formatter);
				}).collect(Collectors.toList());
		return processors;
	}
}
