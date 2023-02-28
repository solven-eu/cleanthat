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
package eu.solven.cleanthat.code_provider.github;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import eu.solven.cleanthat.code_provider.github.event.CompositeCodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.config.CleanthatConfigInitializer;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ICleanthatConfigInitializer;
import eu.solven.cleanthat.config.spring.ConfigSpringConfig;
import eu.solven.cleanthat.engine.ICodeFormatterApplier;
import eu.solven.cleanthat.engine.IEngineFormatterFactory;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.engine.StringFormatterFactory;
import eu.solven.cleanthat.formatter.CodeFormatterApplier;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

/**
 * The {@link Configuration} enabling {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({ ConfigSpringConfig.class })
public class CodeCleanerSpringConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeCleanerSpringConfig.class);

	@Bean
	public ICodeFormatterApplier codeFormatterApplier() {
		return new CodeFormatterApplier();
	}

	@Bean
	public IEngineFormatterFactory stringFormatterFactory(List<IEngineLintFixerFactory> stringFormatters) {
		Map<String, IEngineLintFixerFactory> asMap = new LinkedHashMap<>();

		stringFormatters.forEach(sf -> {
			var language = sf.getEngine();
			LOGGER.info("Formatter registered for engine={}: {}", language, sf);
			asMap.put(language, sf);
		});

		return new StringFormatterFactory(asMap);
	}

	@Bean
	public ICleanthatConfigInitializer configInitializer(ConfigHelpers configHelpers,
			List<IEngineLintFixerFactory> factories) {
		// We exclude OpenRewrite as engine, given it is quite slow and consumes a lot of resources (including through
		// ClassGraph?)
		var cleanFactories =
				factories.stream().filter(f -> !"openrewrite".equals(f.getEngine())).collect(Collectors.toList());
		if (cleanFactories.size() != factories.size()) {
			LOGGER.info("We disabled {} as engine in configInitializer", "openrewrite");
		}

		return new CleanthatConfigInitializer(configHelpers.getObjectMapper(), cleanFactories);
	}

	@Bean
	public ICodeProviderFormatter codeProviderFormatter(ConfigHelpers configHelpers,
			IEngineFormatterFactory formatterFactory,
			ICodeFormatterApplier formatterApplier) {
		return new CodeProviderFormatter(configHelpers, formatterFactory, formatterApplier);
	}

	// @Primary as it is the one to be injected instead of each individual ICodeCleanerFactory
	@Bean
	@Primary
	public ICodeCleanerFactory compositeCodeCleanerFactory(List<ICodeCleanerFactory> specialized) {
		return new CompositeCodeCleanerFactory(specialized);
	}
}
