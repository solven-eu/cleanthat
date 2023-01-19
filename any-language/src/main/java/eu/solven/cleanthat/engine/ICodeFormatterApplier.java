package eu.solven.cleanthat.engine;

import java.io.IOException;

/**
 * Knows how to format a String
 *
 * @author Benoit Lacelle
 */
public interface ICodeFormatterApplier {
	// String getLanguage();

	// String format(ILanguageProperties config, String filepath, String code) throws IOException;

	String applyProcessors(EnginePropertiesAndBuildProcessors languageProperties, String filepath, String code)
			throws IOException;

	// ISourceCodeFormatter makeFormatter(Map<String, ?> rawProcessor, ILanguageProperties languageProperties);
}
