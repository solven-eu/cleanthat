package eu.solven.cleanthat.any_language;

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

	final ObjectMapper objectMapper;
	final ICodeProviderFormatter formatterProvider;

	public ACodeCleaner(ObjectMapper objectMapper, ICodeProviderFormatter formatterProvider) {
		this.objectMapper = objectMapper;
		this.formatterProvider = formatterProvider;
	}

	public Map<String, ?> formatCode(CleanthatRepositoryProperties properties, ICodeProviderWriter pr) {
		return formatterProvider.formatCode(properties, pr);
	}

	@Override
	public Map<String, ?> formatCodeGivenConfig(ICodeProviderWriter codeProvider) {
		Optional<Map<String, ?>> optPrConfig = safeConfig(codeProvider);

		Optional<Map<String, ?>> optConfigurationToUse;
		if (optPrConfig.isEmpty()) {
			throw new IllegalStateException("We should have thrown earlier");
		} else {
			optConfigurationToUse = optPrConfig;
		}
		Optional<String> version = PepperMapHelper.getOptionalString(optConfigurationToUse.get(), "syntax_version");
		if (version.isEmpty()) {
			throw new IllegalStateException("We should have thrown earlier");
		} else if (!"2".equals(version.get())) {
			throw new IllegalStateException("We should have thrown earlier");
		}
		Map<String, ?> prConfig = optConfigurationToUse.get();
		CleanthatRepositoryProperties properties = prepareConfiguration(prConfig);

		if (codeProvider instanceof IListOnlyModifiedFiles) {
			// We are on a PR event, or a commit_push over a branch which is head of an open PR
			LOGGER.info("About to clean a limitted set of files");
		} else {
			LOGGER.info("About to clean the whole repo");
		}
		return formatCode(properties, codeProvider);
	}

	/**
	 * Used to migrate the configuration file automatically. Typically to handle changes of keys.
	 * 
	 * @param properties
	 */
	protected void migrateConfigurationCode(CleanthatRepositoryProperties properties) {
		LOGGER.info("TODO: {}", properties);
	}

	protected CleanthatRepositoryProperties prepareConfiguration(Map<String, ?> prConfig) {
		return CleanthatConfigHelper.parseConfig(objectMapper, prConfig);
	}

	protected Optional<Map<String, ?>> safeConfig(ICodeProvider codeProvider) {
		try {
			return new CodeProviderHelpers(objectMapper).unsafeConfig(codeProvider);
		} catch (RuntimeException e) {
			LOGGER.warn("Issue loading the configuration", e);
			return Optional.empty();
		}
	}
}
