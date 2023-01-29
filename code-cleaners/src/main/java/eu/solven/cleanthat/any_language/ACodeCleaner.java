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
package eu.solven.cleanthat.any_language;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.codeprovider.CodeProviderDecoratingWriter;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatConfigHelper;
import eu.solven.cleanthat.utils.ResultOrError;
import eu.solven.pepper.collection.PepperMapHelper;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asbtract for {@link ICodeCleaner}
 *
 * @author Benoit Lacelle
 */
public abstract class ACodeCleaner implements ICodeCleaner {
	private static final Logger LOGGER = LoggerFactory.getLogger(ACodeCleaner.class);

	final Collection<ObjectMapper> objectMappers;
	private final Collection<IEngineLintFixerFactory> factories;
	final ICodeProviderFormatter formatterProvider;

	public ACodeCleaner(Collection<ObjectMapper> objectMappers,
			Collection<IEngineLintFixerFactory> factories,
			ICodeProviderFormatter formatterProvider) {
		this.objectMappers = objectMappers;
		this.factories = factories;
		this.formatterProvider = formatterProvider;
	}

	protected Collection<ObjectMapper> getObjectMappers() {
		return objectMappers;
	}

	public CodeFormatResult formatCode(CleanthatRepositoryProperties properties,
			ICodeProviderWriter pr,
			boolean dryRun) {
		return formatterProvider.formatCode(properties, pr, dryRun);
	}

	public ResultOrError<CleanthatRepositoryProperties, String> loadAndCheckConfiguration(ICodeProvider codeProvider) {
		Optional<Map<String, ?>> optPrConfig = safeConfig(codeProvider);
		if (optPrConfig.isEmpty()) {
			LOGGER.info("There is no configuration ({}) on {}", CodeProviderHelpers.PATHES_CLEANTHAT, codeProvider);
			return ResultOrError.error("No configuration");
		}
		Optional<String> version = PepperMapHelper.getOptionalString(optPrConfig.get(), "syntax_version");
		if (version.isEmpty()) {
			LOGGER.warn("No version on configuration applying to PR {}", codeProvider);
			return ResultOrError.error("No syntax_version in configuration");
		} else if (!CleanthatRepositoryProperties.LATEST_SYNTAX_VERSION.equals(version.get())) {
			LOGGER.warn("Version '{}' on configuration is not supported {}" + "(only syntax_version='"
					+ CleanthatRepositoryProperties.LATEST_SYNTAX_VERSION
					+ "')", version.get(), codeProvider);
			return ResultOrError.error("Invalid syntax_version in configuration");
		}
		Map<String, ?> prConfig = optPrConfig.get();
		CleanthatRepositoryProperties properties;
		try {
			properties = prepareConfiguration(prConfig);
		} catch (RuntimeException e) {
			// TODO Send a notification, or open a PR requesting to fix the documentation
			throw new IllegalStateException("The configuration file seems invalid", e);
		}
		return ResultOrError.result(properties);
	}

	@Override
	public CodeFormatResult formatCodeGivenConfig(ICodeProviderWriter codeProvider, boolean dryRun) {
		ResultOrError<CleanthatRepositoryProperties, String> optResult = loadAndCheckConfiguration(codeProvider);

		if (optResult.getOptError().isPresent()) {
			throw new IllegalStateException("Issue with configuration: " + optResult.getOptError().get());
		}

		CleanthatRepositoryProperties properties = optResult.getOptResult().get();

		if (codeProvider instanceof IListOnlyModifiedFiles || codeProvider instanceof CodeProviderDecoratingWriter
				&& ((CodeProviderDecoratingWriter) codeProvider).getDecorated() instanceof IListOnlyModifiedFiles) {
			// We are on a PR event, or a commit_push over a branch which is head of an open PR
			LOGGER.info("About to clean a limitted set of files");
		} else {
			LOGGER.info("About to clean the whole repo");
		}
		return formatCode(properties, codeProvider, dryRun);
	}

	/**
	 * Used to migrate the configuration file automatically. Typically to handle changes of key names.
	 * 
	 * @param properties
	 */
	protected void migrateConfigurationCode(CleanthatRepositoryProperties properties) {
		LOGGER.debug("TODO Migration (if necessary) of configuration: {}", properties);
	}

	protected CleanthatRepositoryProperties prepareConfiguration(Map<String, ?> prConfig) {
		// Whatever objectMapper is OK as we do not transcode into json/yaml
		ObjectMapper objectMapper = objectMappers.iterator().next();

		return CleanthatConfigHelper.parseConfig(objectMapper, prConfig);
	}

	protected Optional<Map<String, ?>> safeConfig(ICodeProvider codeProvider) {
		try {
			return new CodeProviderHelpers(objectMappers).unsafeConfig(codeProvider);
		} catch (RuntimeException e) {
			LOGGER.warn("Issue loading the configuration", e);
			return Optional.empty();
		}
	}

	public Collection<IEngineLintFixerFactory> getFactories() {
		return factories;
	}
}
