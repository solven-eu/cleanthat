package eu.solven.cleanthat.formatter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ISkippable;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.LanguagePropertiesAndBuildProcessors;

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

	public LanguagePropertiesAndBuildProcessors compile(ILanguageProperties languageProperties,
			ICodeProvider codeProvider,
			ILanguageLintFixerFactory lintFixerFactory) {
		List<Map.Entry<ILanguageProperties, ILintFixer>> processors =
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
					ILanguageProperties mergedLanguageProperties =
							new ConfigHelpers(Collections.singleton(objectMapper))
									.mergeLanguageIntoProcessorProperties(languageProperties, rawProcessor);
					ILintFixer formatter =
							lintFixerFactory.makeLintFixer(rawProcessor, languageProperties, codeProvider);
					return Maps.immutableEntry(mergedLanguageProperties, formatter);
				}).collect(Collectors.toList());

		List<IStyleEnforcer> codeStyleFixer = processors.stream()
				.map(e -> e.getValue())
				.filter(lf -> lf instanceof IStyleEnforcer)
				.map(lf -> (IStyleEnforcer) lf)
				.collect(Collectors.toList());

		if (codeStyleFixer.isEmpty()) {
			if (reportedLackOfStyleEnforcer.getAndSet(true)) {
				// Already reported
				LOGGER.debug("It is certainly unsafe not to have a single {}", IStyleEnforcer.class.getSimpleName());
			} else {
				LOGGER.warn("It is certainly unsafe not to have a single {}", IStyleEnforcer.class.getSimpleName());
			}
		} else {
			int nbCodeStyleFormatter = codeStyleFixer.size();
			if (nbCodeStyleFormatter >= 2) {
				LOGGER.warn("It is unsual to have multiple {} ({})",
						IStyleEnforcer.class.getSimpleName(),
						nbCodeStyleFormatter);
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

		return new LanguagePropertiesAndBuildProcessors(processors);
	}
}
