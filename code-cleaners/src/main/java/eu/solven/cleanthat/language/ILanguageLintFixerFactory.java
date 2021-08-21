package eu.solven.cleanthat.language;

import java.util.Map;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ILintFixer;

/**
 * Knows how to format a String
 *
 * @author Benoit Lacelle
 */
public interface ILanguageLintFixerFactory {
	String getLanguage();

	ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider);
}
