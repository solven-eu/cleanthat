package eu.solven.cleanthat.language;

import java.util.Map;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ISourceCodeFormatter;

/**
 * Knows how to format a String
 *
 * @author Benoit Lacelle
 */
public interface ISourceCodeFormatterFactory {
	String getLanguage();
	//
	// // String format(ILanguageProperties config, String filepath, String code) throws IOException;
	//
	// String applyProcessors(LanguagePropertiesAndBuildProcessors languageProperties, String filepath, String code)
	// throws IOException;

	ISourceCodeFormatter makeFormatter(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider);
}
