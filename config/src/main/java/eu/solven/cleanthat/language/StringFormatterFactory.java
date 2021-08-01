package eu.solven.cleanthat.language;

import java.util.Map;

/**
 * Default implementation for {@link IStringFormatterFactory}
 * 
 * @author Benoit Lacelle
 *
 */
public class StringFormatterFactory implements IStringFormatterFactory {

	final Map<String, IStringFormatter> languageToFormatter;

	public StringFormatterFactory(Map<String, IStringFormatter> languageToFormatter) {
		this.languageToFormatter = languageToFormatter;
	}

	@Override
	public IStringFormatter makeStringFormatter(ILanguageProperties languageProperties) {
		String language = languageProperties.getLanguage();
		IStringFormatter formatter = languageToFormatter.get(language);

		if (formatter == null) {
			throw new IllegalArgumentException(
					"There is no formatter for language=" + language + " languages=" + languageToFormatter.keySet());
		}

		return formatter;
	}

}
