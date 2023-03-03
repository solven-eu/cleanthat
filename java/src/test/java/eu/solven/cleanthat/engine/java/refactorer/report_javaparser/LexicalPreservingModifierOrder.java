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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.google.common.collect.ImmutableList;

public class LexicalPreservingModifierOrder {
	static final String testCase = "package org.eclipse.mat.snapshot.model;\n" + "\n"
			+ "import java.io.Serializable;\n"
			+ "\n"
			+ "import org.eclipse.mat.internal.Messages;\n"
			+ "\n"
			+ "abstract public class GCRootInfo implements Serializable {\n"
			+ "	private static final long serialVersionUID = 2L;\n"
			+ "\n"
			+ "}\n"
			+ "";

	private static final List<String> ORDERED_MODIFIERS = ImmutableList.of("public",
			"protected",
			"private",
			"abstract",
			"default",
			"static",
			"final",
			"transient",
			"volatile",
			"synchronized",
			"native",
			"strictfp");

	public static void main(String[] args) {
		var unit = StaticJavaParser.parse(testCase);

		unit = LexicalPreservingPrinter.setup(unit);

		var clazz = unit.findFirst(ClassOrInterfaceDeclaration.class).get();
		NodeList<Modifier> modifiers = clazz.getModifiers();

		NodeList<Modifier> mutableModifiers = new NodeList<>(modifiers);

		Collections.sort(mutableModifiers, new Comparator<Modifier>() {

			@Override
			public int compare(Modifier o1, Modifier o2) {
				return compare2(o1.getKeyword().asString(), o2.getKeyword().asString());
			}

			private int compare2(String left, String right) {
				return Integer.compare(ORDERED_MODIFIERS.indexOf(left), ORDERED_MODIFIERS.indexOf(right));
			}
		});

		clazz.setModifiers(mutableModifiers);

		System.out.println("OK");
		System.out.println(unit.toString());

		System.out.println("KO");
		System.out.println(LexicalPreservingPrinter.print(unit));
	}
}
