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
 * Most {@link AJavaParserMutator} will trigger over an {@link Statement}
 *
 * @author Benoit Lacelle
 */
public abstract class AJavaParserStmtMutator extends AJavaParserMutator {
	@Override
	protected boolean processNotRecursively(Node node) {
		if (node instanceof Statement) {
			Statement expr = (Statement) node;

			return processNotRecursively(expr);
		} else {
			return false;
		}

	}

	protected boolean processNotRecursively(Statement stmt) {
		Optional<Expression> optReplacement = replaceStatement(stmt);

		if (optReplacement.isPresent()) {
			Expression replacement = optReplacement.get();
			return tryReplace(stmt, replacement);
		} else {
			return false;
		}
	}

	protected Optional<Expression> replaceStatement(Statement stmt) {
		throw new UnsupportedOperationException("TODO Implement me in overriden classes");
	}
}
