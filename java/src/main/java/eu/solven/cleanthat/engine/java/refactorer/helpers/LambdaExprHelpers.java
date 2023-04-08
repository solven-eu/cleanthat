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

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;

/**
 * Helps crafting {@link LambdaExpr}
 * 
 * @author Benoit Lacelle
 *
 */
public class LambdaExprHelpers {
	protected LambdaExprHelpers() {
		// hidden
	}

	public static Optional<LambdaExpr> makeLambdaExpr(SimpleName simpleName, Statement statement) {
		var parameter = new Parameter(new UnknownType(), simpleName);
		if (statement.isBlockStmt()) {
			return Optional.of(new LambdaExpr(parameter, statement.asBlockStmt()));
		} else if (statement.isExpressionStmt()) {
			return Optional.of(new LambdaExpr(parameter, statement.asExpressionStmt().getExpression()));
		} else {
			return Optional.empty();
		}
	}
}
