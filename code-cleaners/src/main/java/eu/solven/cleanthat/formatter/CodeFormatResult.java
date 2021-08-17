package eu.solven.cleanthat.formatter;

import java.util.Map;

/**
 * Holds a resumee of a codeFormat
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeFormatResult {
	final boolean empty;
	final Map<String, ?> details;

	public CodeFormatResult(boolean empty, Map<String, ?> details) {
		this.empty = empty;
		this.details = details;
	}

	public boolean isEmpty() {
		return empty;
	}
}
