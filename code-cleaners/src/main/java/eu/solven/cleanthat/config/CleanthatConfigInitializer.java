package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.RepoInitializerResult.RepoInitializerResultBuilder;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.pepper.resource.PepperResourceHelper;

/**
 * This will help configuration CleanThat by proposing a reasonnable default configuration.
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanthatConfigInitializer {
	final ICodeProvider codeProvider;
	final ObjectMapper objectMapper;
	final Collection<IEngineLintFixerFactory> factories;

	public CleanthatConfigInitializer(ICodeProvider codeProvider,
			ObjectMapper objectMapper,
			Collection<IEngineLintFixerFactory> factories) {
		this.codeProvider = codeProvider;
		this.objectMapper = objectMapper;
		this.factories = factories;
	}

	public RepoInitializerResult prepareFile() {
		String defaultRepoPropertiesPath = CodeProviderHelpers.PATHES_CLEANTHAT.get(0);

		// Let's follow Renovate and its configuration PR
		// https://github.com/solven-eu/agilea/pull/1
		String body = PepperResourceHelper.loadAsString("/templates/onboarding-body.md");
		// body = body.replaceAll(Pattern.quote("${REPO_FULL_NAME}"), repo.getFullName());
		body = body.replaceAll(Pattern.quote("${DEFAULT_PATH}"), defaultRepoPropertiesPath);

		RepoInitializerResultBuilder resultBuilder = RepoInitializerResult.builder()
				.prBody(body)
				.commitMessage(PepperResourceHelper.loadAsString("/templates/commit-message.txt"));

		GenerateInitialConfig generateInitialConfig = new GenerateInitialConfig(factories);
		EngineInitializerResult repoProperties;
		try {
			repoProperties = generateInitialConfig.prepareDefaultConfiguration(codeProvider);

			String repoPropertiesYaml = objectMapper.writeValueAsString(repoProperties.getRepoProperties());
			resultBuilder.pathToContent(defaultRepoPropertiesPath, repoPropertiesYaml);

			// Register the custom files of the engine
			repoProperties.getPathToContents().forEach(resultBuilder::pathToContent);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue preparing initial config given codeProvider=" + codeProvider, e);
		}

		return resultBuilder.build();
	}

}
