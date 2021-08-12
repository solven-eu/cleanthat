package eu.solven.cleanthat.language;

/**
 * Make {@link ICodeFormatterApplier} for different languages.
 * 
 * @author Benoit Lacelle
 *
 */
public interface ILanguageFormatterFactory {
	ISourceCodeFormatterFactory makeLanguageFormatter(ILanguageProperties languageProperties);
}
