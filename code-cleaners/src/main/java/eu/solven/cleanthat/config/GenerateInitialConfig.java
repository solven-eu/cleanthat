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
package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AtomicLongMap;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.engine.IEngineStep;

/**
 * Helps generating a default {@link CleanthatRepositoryProperties}
 * 
 * @author Benoit Lacelle
 *
 */
public class GenerateInitialConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateInitialConfig.class);

	final Collection<IEngineLintFixerFactory> factories;

	public GenerateInitialConfig(Collection<IEngineLintFixerFactory> factories) {
		this.factories = factories;
	}

	// Guess Java version: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L13
	// Detect usage of Checkstyle:
	// https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L35
	// Code formatting: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L17
	// https://github.com/spring-io/spring-javaformat/blob/master/src/checkstyle/checkstyle.xml
	// com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck
	public EngineInitializerResult prepareDefaultConfiguration(ICodeProvider codeProvider) throws IOException {
		Map<String, String> pathToContent = new LinkedHashMap<>();

		var properties = CleanthatRepositoryProperties.defaultRepository();

		List<CleanthatEngineProperties> mutableEngines = new ArrayList<>(properties.getEngines());
		properties.setEngines(mutableEngines);

		var factoryToFileCount = scanFileExtentions(codeProvider,
				factories,
				IEngineLintFixerFactory::getEngine,
				IEngineLintFixerFactory::getDefaultIncludes);

		factoryToFileCount.asMap().forEach((engine, count) -> {
			if (count == 0) {
				LOGGER.info("Not a single file matched {}", engine);
			} else {
				LOGGER.info("Some files ({}) matched {}", count, engine);

				var factory = factories.stream().filter(f -> engine.equals(f.getEngine())).findAny().get();

				var stepToFileCount = scanFileExtentions(codeProvider,
						factory.getMainSteps(),
						IEngineStep::getStep,
						IEngineStep::getDefaultIncludes);

				var subStepIds = stepToFileCount.asMap().keySet();
				LOGGER.info("We accept subStepIds={} for {}", subStepIds, engine);
				var engineProperties = factory.makeDefaultProperties(subStepIds);
				mutableEngines.add(engineProperties);

				pathToContent.putAll(factory.makeCustomDefaultFiles(engineProperties, subStepIds));
			}
		});

		return EngineInitializerResult.builder().repoProperties(properties).pathToContents(pathToContent).build();
	}

	// PMD.CloseResource: False positive as we did not open it ourselves
	@SuppressWarnings("PMD.CloseResource")
	public <T> AtomicLongMap<String> scanFileExtentions(ICodeProvider codeProvider,
			Collection<T> factories,
			Function<T, String> getId,
			Function<T, Set<String>> getDefaultIncludes) {
		AtomicLongMap<String> factoryToFileCount = AtomicLongMap.create();

		var repoRoot = codeProvider.getRepositoryRoot();

		try {
			// Listing files may be slow if there is many files (e.g. download of repo as zip)
			LOGGER.info("About to list files to prepare a default configuration");

			Set<String> allIncludes = new HashSet<>();

			factories.forEach(f -> allIncludes.addAll(getDefaultIncludes.apply(f)));

			codeProvider.listFilesForFilenames(allIncludes, file -> {
				var filePath = file.getPath();

				factories.forEach(factory -> {
					Set<String> includes = getDefaultIncludes.apply(factory);

					var includeMatchers = IncludeExcludeHelpers.prepareMatcher(repoRoot.getFileSystem(), includes);
					var matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

					if (matchingInclude.isPresent()) {
						factoryToFileCount.getAndIncrement(getId.apply(factory));
					}
				});
			});
		} catch (IOException e) {
			throw new UncheckedIOException("Issue listing all files for extentions", e);
		} catch (OutOfMemoryError e) {
			// https://github.com/hub4j/github-api/issues/1405
			// The implementation downloading the repo as zip materialized the whole zip in memory
			LOGGER.warn("Issue while processing the repository", e);
		}

		LOGGER.info("Extentions found in {}: {}", codeProvider, factoryToFileCount);

		return factoryToFileCount;
	}

}
