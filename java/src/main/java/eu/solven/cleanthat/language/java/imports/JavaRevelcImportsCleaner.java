/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.language.java.imports;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import net.revelc.code.impsort.Grouper;
import net.revelc.code.impsort.ImpSort;
import net.revelc.code.impsort.Result;

/**
 * Bridges to Eclipse formatting engine
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
public class JavaRevelcImportsCleaner implements ISourceCodeFormatter {

	private static final Logger LOGGER = LoggerFactory.getLogger(JavaRevelcImportsCleaner.class);

	final ISourceCodeProperties sourceCodeProperties;

	final JavaRevelcImportsCleanerProperties properties;

	public JavaRevelcImportsCleaner(ISourceCodeProperties sourceCodeProperties,
			JavaRevelcImportsCleanerProperties properties) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.properties = properties;
	}

	@Override
	public String doFormat(String code, LineEnding eolToApply) throws IOException {
		// see net.revelc.code.impsort.maven.plugin.AbstractImpSortMojo
		Grouper grouper = new Grouper(properties.getGroups(), properties.getStaticGroups(), false, false, true);
		Charset charset = Charset.forName(sourceCodeProperties.getEncoding());
		ImpSort impsort = new ImpSort(charset,
				grouper,
				properties.isRemoveUnused(),
				true,
				net.revelc.code.impsort.LineEnding.valueOf(eolToApply.name()));
		Path tmpFile = Files.createTempFile("cleanthat", ".tmp");
		try {
			Files.writeString(tmpFile, code, charset, StandardOpenOption.TRUNCATE_EXISTING);
			Result result = impsort.parseFile(tmpFile);
			if (!result.isSorted()) {
				LOGGER.debug("Saving imports-sorted file to {}", tmpFile);
				result.saveSorted(tmpFile);
				LOGGER.debug("Loading imports-sorted file to {}", tmpFile);
				String newCode = new String(Files.readAllBytes(tmpFile), charset);
				if (newCode.equals(code)) {
					LOGGER.info("Sorted imports (with no impact ???)");
				} else {
					LOGGER.info("Sorted imports (with an impact)");
				}
				code = newCode;
			}
		} finally {
			tmpFile.toFile().delete();
		}
		return code;
	}
}
