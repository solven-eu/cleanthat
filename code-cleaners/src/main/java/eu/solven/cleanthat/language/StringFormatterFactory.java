package eu.solven.cleanthat.language;

import java.util.Map;

/**
 * Default implementation for {@link ILanguageFormatterFactory}
 * 
 * @author Benoit Lacelle
 *
 */
public class StringFormatterFactory implements ILanguageFormatterFactory {

	final Map<String, ISourceCodeFormatterFactory> languageToFormatter;

	public StringFormatterFactory(Map<String, ISourceCodeFormatterFactory> languageToFormatter) {
		this.languageToFormatter = languageToFormatter;
	}

	@Override
	public ISourceCodeFormatterFactory makeLanguageFormatter(ILanguageProperties languageProperties) {
		String language = languageProperties.getLanguage();
		ISourceCodeFormatterFactory formatter = languageToFormatter.get(language);

		if (formatter == null) {
			throw new IllegalArgumentException(
					"There is no formatter for language=" + language + " languages=" + languageToFormatter.keySet());
		}

		return formatter;
	}

}
