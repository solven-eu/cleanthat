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
package eu.solven.cleanthat.java.imports;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.formatter.eclipse.ICodeProcessor;
import net.revelc.code.impsort.Grouper;
import net.revelc.code.impsort.ImpSort;
import net.revelc.code.impsort.Result;

/**
 * Bridges to Eclipse formatting engine
 * 
 * @author Benoit Lacelle
 *
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
public class JavaRevelcImportsCleaner implements ICodeProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaRevelcImportsCleaner.class);

	private final JavaRevelcImportsCleanerProperties properties;

	public JavaRevelcImportsCleaner(JavaRevelcImportsCleanerProperties properties) {
		this.properties = properties;
	}

	public String doFormat(String code, LineEnding eolToApply) throws IOException {
		// see net.revelc.code.impsort.maven.plugin.AbstractImpSortMojo
		Grouper grouper = new Grouper(properties.getGroups(), properties.getStaticGroups(), false, false, true);
		Charset charset = Charset.forName(properties.getEncoding());
		ImpSort impsort = new ImpSort(charset,
				grouper,
				properties.isRemoveUnusedImports(),
				true,
				net.revelc.code.impsort.LineEnding.valueOf(eolToApply.name()));

		Path tmpFile = Files.createTempFile("cleanthat", ".tmp");
		Files.writeString(tmpFile, code, charset, StandardOpenOption.TRUNCATE_EXISTING);

		Result result = impsort.parseFile(tmpFile);
		if (!result.isSorted()) {
			LOGGER.info("Saving imports-sorted file to {}", tmpFile);
			result.saveSorted(tmpFile);
			LOGGER.info("Loading imports-sorted file to {}", tmpFile);
			code = new String(Files.readAllBytes(tmpFile), charset);
		}

		return code;
	}

}