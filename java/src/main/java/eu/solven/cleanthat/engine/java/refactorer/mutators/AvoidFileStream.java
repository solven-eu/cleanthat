/*
 * Copyright 2023 Solven
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
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.ATodoJavaParserRule;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;

/**
 * Avoid use of {@link FileInputStream}, {@link FileOutputStream}, {@link FileReader} and {@link FileWriter}
 * 
 * @author Benoit Lacelle
 *
 */
@Deprecated(since = "Not-ready")
public class AvoidFileStream extends ATodoJavaParserRule implements IClassTransformer {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_7;
	}

	@Override
	public String sonarUrl() {
		return "";
	}

	@Override
	public String pmdUrl() {
		// PMD.AvoidFileStream
		return "https://pmd.github.io/latest/pmd_rules_java_performance.html#avoidfilestream";
	}

}
