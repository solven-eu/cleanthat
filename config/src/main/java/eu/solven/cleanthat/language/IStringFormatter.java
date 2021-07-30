package eu.solven.cleanthat.language;

import java.io.IOException;

/**
 * Knows how to format a String
 *
 * @author Benoit Lacelle
 */
public interface IStringFormatter {
	String getLanguage();

	String format(ILanguageProperties config, String filepath, String code) throws IOException;
}
