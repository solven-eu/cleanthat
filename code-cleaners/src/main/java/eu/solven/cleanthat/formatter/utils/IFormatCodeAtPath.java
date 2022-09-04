package eu.solven.cleanthat.formatter.utils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Typically used by formatters requiring an input Path (instead of a String)
 * 
 * @author Benoit Lacelle
 *
 */
public interface IFormatCodeAtPath {
	String formatPath(Path path) throws IOException;
}
