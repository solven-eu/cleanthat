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
package eu.solven.cleanthat.engine.java.refactorer.helpers;

import java.util.Optional;

import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.Statement;

/**
 * Helps working with {@link VariableDeclarationExpr}
 * 
 * @author Benoit Lacelle
 *
 */
public class VariableDeclarationExprHepers {
	protected VariableDeclarationExprHepers() {
		// hidden
	}

	/**
	 * 
	 * @param stmt
	 * @return the {@link VariableDeclarationExpr} if stmt is semantically equivalent to a simple
	 *         VariableDeclarationExpr;
	 */
	public static Optional<VariableDeclarationExpr> optSimpleDeclaration(Statement stmt) {
		if (!stmt.isExpressionStmt() || !stmt.asExpressionStmt().getExpression().isVariableDeclarationExpr()) {
			return Optional.empty();
		}
		var assignExpr = stmt.asExpressionStmt().getExpression().asVariableDeclarationExpr();
		if (assignExpr.getVariables().size() != 1) {
			return Optional.empty();
		}

		return Optional.of(assignExpr);
	}
}
