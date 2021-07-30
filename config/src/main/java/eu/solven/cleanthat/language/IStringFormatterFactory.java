package eu.solven.cleanthat.language;

/**
 * Make {@link IStringFormatter} for different languages.
 * 
 * @author Benoit Lacelle
 *
 */
public interface IStringFormatterFactory {
	IStringFormatter makeStringFormatter(ILanguageProperties languageProperties);
}
