package eu.solven.cleanthat.code_provider.github.code_provider;

import java.io.IOException;

/**
 * To be used when something failed due to a too-big file
 * 
 * @author Benoit Lacelle
 *
 */
public class FileIsTooBigException extends IOException {
	private static final long serialVersionUID = 9181885352021915226L;

	final long length;

	public FileIsTooBigException(String message, long length, Throwable cause) {
		super(message, cause);

		this.length = length;
	}

	public FileIsTooBigException(String message, long length) {
		super(message);

		this.length = length;
	}

	public long getLength() {
		return length;
	}

}
