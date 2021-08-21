package eu.solven.cleanthat.language;

import java.util.Map;

/**
 * Default implementation for {@link ILanguageFormatterFactory}
 * 
 * @author Benoit Lacelle
 *
 */
public class StringFormatterFactory implements ILanguageFormatterFactory {

	final Map<String, ILanguageLintFixerFactory> languageToFormatter;

	public StringFormatterFactory(Map<String, ILanguageLintFixerFactory> languageToFormatter) {
		this.languageToFormatter = languageToFormatter;
	}

	@Override
	public ILanguageLintFixerFactory makeLanguageFormatter(ILanguageProperties languageProperties) {
		String language = languageProperties.getLanguage();
		ILanguageLintFixerFactory formatter = languageToFormatter.get(language);

		if (formatter == null) {
			throw new IllegalArgumentException(
					"There is no formatter for language=" + language + " languages=" + languageToFormatter.keySet());
		}

		return formatter;
	}

}
