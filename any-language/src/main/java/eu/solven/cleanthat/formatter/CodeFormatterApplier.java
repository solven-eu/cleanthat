package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.config.IncludeExcludeHelpers;
import eu.solven.cleanthat.language.ICodeFormatterApplier;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.cleanthat.language.LanguagePropertiesAndBuildProcessors;

/**
 * Abstract class for language formatters
 *
 * @author Benoit Lacelle
 */
public class CodeFormatterApplier implements ICodeFormatterApplier {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeFormatterApplier.class);

	@Override
	public String applyProcessors(LanguagePropertiesAndBuildProcessors languagePropertiesAndProcessors,
			String filepath,
			String code) throws IOException {
		AtomicReference<String> outputRef = new AtomicReference<>(code);

		languagePropertiesAndProcessors.getProcessors().forEach(rawProcessor -> {
			try {
				String input = outputRef.get();
				ILanguageProperties languageProperties = rawProcessor.getKey();
				ILintFixer lintFixer = rawProcessor.getValue();
				String output = applyProcessor(languageProperties, lintFixer, filepath, input);
				if (output == null) {
					throw new IllegalStateException("Null code. TODO");
				}
				if (!input.equals(output)) {
					// Beware each processor may change a file, but the combined changes leads to a no change (e.g. the
					// final formatting step clean all previous not relevant changes)
					LOGGER.debug("{} mutated a file", rawProcessor);
				}
				outputRef.set(output);
			} catch (IOException | RuntimeException e) {
				// Log and move to next processor
				LOGGER.warn("Issue with " + rawProcessor, e);
			}
		});
		return outputRef.get();
	}

	protected String applyProcessor(ILanguageProperties languageProperties,
			ILintFixer formatter,
			String filepath,
			String code) throws IOException {
		Objects.requireNonNull(code, "code should not be null");
		ISourceCodeProperties sourceCodeProperties = languageProperties.getSourceCode();

		List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getIncludes());
		Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filepath);
		if (matchingInclude.isEmpty()) {
			LOGGER.debug("File {} was initially included but not included for processor: {}", filepath, formatter);
			return code;
		}

		List<PathMatcher> excludeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getExcludes());
		Optional<PathMatcher> matchingExclude = IncludeExcludeHelpers.findMatching(excludeMatchers, filepath);
		if (matchingExclude.isPresent()) {
			LOGGER.debug("File {} was initially not-excluded but excluded for processor: {}", filepath, formatter);
			return code;
		}
		LineEnding lineEnding = languageProperties.getSourceCode().getLineEndingAsEnum();
		return formatter.doFormat(code, lineEnding);
	}
}
