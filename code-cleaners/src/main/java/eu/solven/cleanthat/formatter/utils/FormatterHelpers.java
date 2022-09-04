package eu.solven.cleanthat.formatter.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Helps during formatting operations
 * 
 * @author Benoit Lacelle
 *
 */
public class FormatterHelpers {
	protected FormatterHelpers() {
		// hidden
	}

	/**
	 * Helps the process of formatting some code when the procedure requires it to be written in a {@link File}.
	 * 
	 * @param code
	 * @param charset
	 * @param formatCodeAtPath
	 * @return
	 * @throws IOException
	 */
	public static String formatAsFile(String code, Charset charset, IFormatCodeAtPath formatCodeAtPath)
			throws IOException {
		Path tmpFile = Files.createTempFile("cleanthat", ".tmp");
		try {
			Files.writeString(tmpFile, code, charset, StandardOpenOption.TRUNCATE_EXISTING);
			return formatCodeAtPath.formatPath(tmpFile);
		} finally {
			tmpFile.toFile().delete();
		}
	}
}
