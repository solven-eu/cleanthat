package eu.solven.cleanthat.language;

import java.io.IOException;

/**
 * Knows how to format a String
 *
 * @author Benoit Lacelle
 */
public interface ICodeFormatterApplier {
	// String getLanguage();

	// String format(ILanguageProperties config, String filepath, String code) throws IOException;

	String applyProcessors(LanguagePropertiesAndBuildProcessors languageProperties, String filepath, String code)
			throws IOException;

	// ISourceCodeFormatter makeFormatter(Map<String, ?> rawProcessor, ILanguageProperties languageProperties);
}
