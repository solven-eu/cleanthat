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
package eu.solven.cleanthat.any_language;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.codeprovider.CodeProviderDecoratingWriter;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;
import eu.solven.cleanthat.config.ICleanthatConfigConstants;
import eu.solven.cleanthat.config.ICleanthatConfigInitializer;
import eu.solven.cleanthat.config.RepoInitializerResult;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatConfigHelper;
import eu.solven.cleanthat.utils.ResultOrError;
import eu.solven.pepper.collection.PepperMapHelper;

/**
 * Asbtract for {@link ICodeCleaner}
 *
 * @author Benoit Lacelle
 */
public abstract class ACodeCleaner implements ICodeCleaner {
	private static final Logger LOGGER = LoggerFactory.getLogger(ACodeCleaner.class);

	final Collection<ObjectMapper> objectMappers;
	final ICleanthatConfigInitializer configInitializer;
	final ICodeProviderFormatter formatterProvider;

	public ACodeCleaner(Collection<ObjectMapper> objectMappers,
			ICleanthatConfigInitializer configInitializer,
			ICodeProviderFormatter formatterProvider) {
		this.objectMappers = objectMappers;
		this.configInitializer = configInitializer;
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
		var optPrConfig = safeConfig(codeProvider);
		if (optPrConfig.isEmpty()) {
			LOGGER.info("There is no configuration ({}) on {}",
					ICleanthatConfigConstants.PATHES_CLEANTHAT,
					codeProvider);
			return ResultOrError.error("No configuration");
		}
		var version = PepperMapHelper.getOptionalString(optPrConfig.get(), "syntax_version");
		if (version.isEmpty()) {
			LOGGER.warn("No version on configuration applying to PR {}", codeProvider);
			return ResultOrError.error("No syntax_version in configuration");
		} else if (!CleanthatRepositoryProperties.LATEST_SYNTAX_VERSION.equals(version.get())) {
			LOGGER.warn("Version '{}' on configuration is not supported {}" + "(only syntax_version='"
					+ CleanthatRepositoryProperties.LATEST_SYNTAX_VERSION
					+ "')", version.get(), codeProvider);
			return ResultOrError.error("Invalid syntax_version in configuration");
		}
		var prConfig = optPrConfig.get();
		CleanthatRepositoryProperties properties;
		try {
			properties = prepareConfiguration(prConfig);
		} catch (RuntimeException e) {
			// TODO Send a notification, or open a PR requesting to fix the documentation
			throw new IllegalStateException("The configuration file seems invalid", e);
		}
		return ResultOrError.result(properties);
	}

	public static boolean isLimittedSetOfFiles(ICodeProvider codeProvider) {
		return codeProvider instanceof IListOnlyModifiedFiles || codeProvider instanceof CodeProviderDecoratingWriter
				&& ((CodeProviderDecoratingWriter) codeProvider).getDecorated() instanceof IListOnlyModifiedFiles;
	}

	@Override
	public CodeFormatResult formatCodeGivenConfig(String eventKey, ICodeProviderWriter codeProvider, boolean dryRun) {
		var optResult = loadAndCheckConfiguration(codeProvider);

		if (optResult.getOptError().isPresent()) {
			throw new IllegalStateException("Issue with configuration: " + optResult.getOptError().get());
		}

		var properties = optResult.getOptResult().get();

		if (isLimittedSetOfFiles(codeProvider)) {
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
		var objectMapper = objectMappers.iterator().next();

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

	protected RepoInitializerResult generateDefaultConfiguration(ICodeProvider codeProvider,
			boolean isPrivateRepo,
			String eventKey) {
		return configInitializer.prepareFile(codeProvider, isPrivateRepo, eventKey);
	}

}
