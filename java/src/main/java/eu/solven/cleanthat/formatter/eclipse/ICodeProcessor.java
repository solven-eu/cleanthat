package eu.solven.cleanthat.formatter.eclipse;

import java.io.IOException;

import eu.solven.cleanthat.formatter.LineEnding;

/**
 * Knows how to format a piece of code
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeProcessor {

	String doFormat(String code, LineEnding ending) throws IOException;

}
