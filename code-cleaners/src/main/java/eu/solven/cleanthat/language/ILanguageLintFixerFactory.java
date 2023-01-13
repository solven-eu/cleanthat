package eu.solven.cleanthat.language;

import java.util.Map;
import java.util.Set;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.pojo.LanguageProperties;
import eu.solven.cleanthat.formatter.ILintFixer;

/**
 * Knows how to format a String
 *
 * @author Benoit Lacelle
 */
public interface ILanguageLintFixerFactory {
	String KEY_ENGINE = "engine";
	String KEY_PARAMETERS = "parameters";

	String getLanguage();

	/**
	 * The typical file extensions concerned by this LintFixer
	 * 
	 * @return the {@link Set} of relevant file extensions.
	 */
	Set<String> getFileExtentions();

	ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider);

	LanguageProperties makeDefaultProperties();
}
