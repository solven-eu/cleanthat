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
package eu.solven.cleanthat.engine.java.refactorer.report_javaparser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

public class LexicalPreservingAnnotatedPackage {
	static final String testCase = "/*\n" + " * Licensed under the Apache License, Version 2.0 (the \"License\");\n"
			+ " * you may not use this file except in compliance with the License.\n"
			+ " * You may obtain a copy of the License at\n"
			+ " */\n"
			+ "\n"
			+ "@XmlSchema(\n"
			+ "		xmlns = {\n"
			+ "				@XmlNs(prefix = \"order\", namespaceURI = \"http://www.camel.apache.org/jaxb/example/order/1\"),\n"
			+ "				@XmlNs(prefix = \"address\", namespaceURI = \"http://www.camel.apache.org/jaxb/example/address/1\")\n"
			+ "		}\n"
			+ ")\n"
			+ "package net.revelc.code.imp;\n"
			+ "\n"
			+ "import net.revelc.code.imp.Something;\n"
			+ "\n"
			+ "@Component\n"
			+ "public class UnusedImports {\n"
			+ "}\n"
			+ "";

	public static void main(String[] args) {
		var node = StaticJavaParser.parse(testCase);

		node = LexicalPreservingPrinter.setup(node);

		node.getImport(0).remove();

		System.out.println(LexicalPreservingPrinter.print(node));
	}
}
