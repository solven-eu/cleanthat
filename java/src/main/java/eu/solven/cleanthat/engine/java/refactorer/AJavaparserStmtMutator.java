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
package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;

/**
 * Most {@link AJavaparserNodeMutator} will trigger over an {@link Statement}
 *
 * @author Benoit Lacelle
 */
public abstract class AJavaparserStmtMutator extends AJavaparserNodeMutator {
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndSymbolSolver) {
		Node node = nodeAndSymbolSolver.getNode();
		if (node instanceof Statement) {
			var stmt = (Statement) nodeAndSymbolSolver.getNode();

			return processStatement(nodeAndSymbolSolver.editNode(stmt));
		} else {
			return false;
		}

	}

	protected boolean processStatement(NodeAndSymbolSolver<Statement> expr) {
		Optional<Expression> optReplacement = replaceStatement(expr);

		if (optReplacement.isPresent()) {
			var replacement = optReplacement.get();
			return tryReplace(expr.getNode(), replacement);
		} else {
			return false;
		}
	}

	protected Optional<Expression> replaceStatement(NodeAndSymbolSolver<Statement> expr) {
		throw new UnsupportedOperationException("TODO Implement me in overriden classes");
	}
}
