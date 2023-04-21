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
package eu.solven.cleanthat.engine.java.refactorer.corrupted;

import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This {@link IMutator} does may modify the AST before cancelling its operation, leaving the Node corrupted.
 *
 * @author Benoit Lacelle
 */
public class CancelAfterMutationMutator extends AJavaparserNodeMutator {
	@Override
	public String getId() {
		return "TryWrappingInMethodCallExpr";
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("UnitTest");
	}

	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndSolver) {
		Node node = nodeAndSolver.getNode();
		if (node instanceof Expression) {
			// Let's fake the provisioning of a MethodCallExpr
			new MethodCallExpr((Expression) node, "someMethod");

			// Finally, we are not happy
			return false;
		}

		return false;
	}
}
