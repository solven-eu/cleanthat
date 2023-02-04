/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
