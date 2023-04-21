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

/**
 * Most {@link AJavaparserAstMutator} will trigger over an {@link Expression}
 *
 * @author Benoit Lacelle
 */
public abstract class AJavaparserExprMutator extends AJavaparserNodeMutator {
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndSymbolSolver) {
		Node node = nodeAndSymbolSolver.getNode();
		if (node instanceof Expression) {
			var expr = (Expression) nodeAndSymbolSolver.getNode();

			return processExpression(nodeAndSymbolSolver.editNode(expr));
		} else {
			return false;
		}

	}

	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		Optional<Expression> optReplacement = replaceExpression(expr);

		if (optReplacement.isPresent()) {
			var replacement = optReplacement.get();
			return tryReplace(expr.getNode(), replacement);
		} else {
			return false;
		}
	}

	protected Optional<Expression> replaceExpression(NodeAndSymbolSolver<Expression> expr) {
		throw new UnsupportedOperationException("TODO Implement me in overriden classes");
	}
}
