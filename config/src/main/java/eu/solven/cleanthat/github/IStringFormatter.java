package eu.solven.cleanthat.github;

import java.io.IOException;

/**
 * Knows how to format a String
 *
 * @author Benoit Lacelle
 */
public interface IStringFormatter {

	String format(ILanguageProperties config, String code) throws IOException;
}
