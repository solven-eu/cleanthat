package eu.solven.cleanthat.code_provider.github;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.event.CodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.config.spring.ConfigSpringConfig;
import eu.solven.cleanthat.formatter.CodeFormatterApplier;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.language.ICodeFormatterApplier;
import eu.solven.cleanthat.language.ILanguageFormatterFactory;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;
import eu.solven.cleanthat.language.StringFormatterFactory;

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
	public ILanguageFormatterFactory stringFormatterFactory(List<ILanguageLintFixerFactory> stringFormatters) {
		Map<String, ILanguageLintFixerFactory> asMap = new LinkedHashMap<>();

		stringFormatters.forEach(sf -> {
			String language = sf.getLanguage();
			LOGGER.info("Formatter registered for language={}: {}", language, sf);
			asMap.put(language, sf);
		});

		return new StringFormatterFactory(asMap);
	}

	@Bean
	public ICodeProviderFormatter codeProviderFormatter(List<ObjectMapper> objectMappers,
			ILanguageFormatterFactory formatterFactory,
			ICodeFormatterApplier formatterApplier) {
		return new CodeProviderFormatter(objectMappers, formatterFactory, formatterApplier);
	}

	@Bean
	public ICodeCleanerFactory codeCleanerFactory(List<ObjectMapper> objectMappers,
			ICodeProviderFormatter formatterProvider) {
		return new CodeCleanerFactory(objectMappers, formatterProvider);
	}
}
