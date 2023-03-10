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
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Turns `int i = 0;;` into `int i = 0;`
 *
 * @author Benoit Lacelle
 */
public class UnnecessarySemicolon extends AJavaparserMutator {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#unnecessarysemicolon";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UnnecessarySemicolon");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-2959");
	}

	@Override
	protected boolean processNotRecursively(Node node) {
		if (node instanceof Statement) {
			var stmt = (Statement) node;
			return processNotRecursively(stmt);
		}

		return false;
	}

	@SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
	protected boolean processNotRecursively(Statement stmt) {
		if (!stmt.isEmptyStmt()) {
			return false;
		}

		var emptyStmt = stmt.asEmptyStmt();

		Optional<Node> parentNode = emptyStmt.getParentNode();
		if (parentNode.isPresent() && parentNode.get() instanceof IfStmt) {
			var ifStmt = (IfStmt) parentNode.get();

			if (ifStmt.getThenStmt().equals(emptyStmt) && ifStmt.getElseStmt().isEmpty()) {
				// Turns `if (l.remove(""));` into `l.remove("");`
				var condition = ifStmt.getCondition();

				// TODO What is the exact rules allowing to convert an expression to a statement?
				if (condition.isMethodCallExpr()) {
					return ifStmt.replace(new ExpressionStmt(condition));
				}
			}
		}

		return stmt.remove();
	}
}
