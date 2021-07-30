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
		return languageToFormatter.get(languageProperties.getLanguage());
	}

}
