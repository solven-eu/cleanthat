package eu.solven.cleanthat.formatter;

import java.io.IOException;

/**
 * Knows how to format a piece of code
 *
 * @author Benoit Lacelle
 */
public interface ISourceCodeFormatter {

	String doFormat(String code, LineEnding ending) throws IOException;
}
