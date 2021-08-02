package eu.solven.cleanthat.any_language;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatConfigHelper;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;

/**
 * Asbtract for {@link IGithubRefCleaner}
 *
 * @author Benoit Lacelle
 */
public abstract class ACodeCleaner implements ICodeCleaner {
	private static final Logger LOGGER = LoggerFactory.getLogger(ACodeCleaner.class);

	final List<ObjectMapper> objectMappers;
	final ICodeProviderFormatter formatterProvider;

	public ACodeCleaner(List<ObjectMapper> objectMappers, ICodeProviderFormatter formatterProvider) {
		this.objectMappers = objectMappers;
		this.formatterProvider = formatterProvider;
	}

	public Map<String, ?> formatCode(CleanthatRepositoryProperties properties, ICodeProviderWriter pr, boolean dryRun) {
		return formatterProvider.formatCode(properties, pr, dryRun);
	}

	@Override
	public Map<String, ?> formatCodeGivenConfig(ICodeProviderWriter codeProvider, boolean dryRun) {
		Optional<Map<String, ?>> optPrConfig = safeConfig(codeProvider);

		Optional<Map<String, ?>> optConfigurationToUse;
		if (optPrConfig.isEmpty()) {
			// In Maven, we do not check earlier the presence of a configuration file
			throw new IllegalStateException(
					"We lack a configuration file (" + CodeProviderHelpers.FILENAME_CLEANTHAT_YAML + ")");
		} else {
			optConfigurationToUse = optPrConfig;
		}
		Optional<String> version = PepperMapHelper.getOptionalString(optConfigurationToUse.get(), "syntax_version");
		if (version.isEmpty()) {
			throw new IllegalStateException("The configuration lacks a 'syntax_version' property");
		} else if (!CleanthatRepositoryProperties.LATEST_SYNTAX_VERSION.equals(version.get())) {
			throw new IllegalStateException("syntax_version=" + version.get()
					+ " is not supported (only syntax_version='"
					+ CleanthatRepositoryProperties.LATEST_SYNTAX_VERSION
					+ "')");
		}
		Map<String, ?> prConfig = optConfigurationToUse.get();
		CleanthatRepositoryProperties properties = prepareConfiguration(prConfig);

		if (codeProvider instanceof IListOnlyModifiedFiles) {
			// We are on a PR event, or a commit_push over a branch which is head of an open PR
			LOGGER.info("About to clean a limitted set of files");
		} else {
			LOGGER.info("About to clean the whole repo");
		}
		return formatCode(properties, codeProvider, dryRun);
	}

	/**
	 * Used to migrate the configuration file automatically. Typically to handle changes of keys.
	 * 
	 * @param properties
	 */
	protected void migrateConfigurationCode(CleanthatRepositoryProperties properties) {
		LOGGER.debug("TODO Migration (if necessary) of configuration: {}", properties);
	}

	protected CleanthatRepositoryProperties prepareConfiguration(Map<String, ?> prConfig) {
		// Whatever objectMapper is OK as we do not transcode into json/yaml
		ObjectMapper objectMapper = objectMappers.get(0);

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
}
