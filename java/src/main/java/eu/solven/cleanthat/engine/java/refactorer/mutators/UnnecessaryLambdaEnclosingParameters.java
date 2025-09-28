/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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

import java.util.Set;

import com.github.javaparser.ast.expr.LambdaExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Turns '.stream((s) -> s.subString(0, 2))' into '.stream(s -> s.subString(0, 2))'
 *
 * @author Benoit Lacelle
 */
public class UnnecessaryLambdaEnclosingParameters extends AJavaparserNodeMutator {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Lambda");
	}

	@Override
	public boolean isDraft() {
		// see TestUnnecessaryLambdaEnclosingParametersCases.CaseFunction
		return true;
	}

	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof LambdaExpr)) {
			return false;
		}

		var lambdaExpr = (LambdaExpr) node.getNode();
		if (lambdaExpr.getParameters().size() != 1 || lambdaExpr.isExplicitlyTyped()) {
			return false;
		}

		var newLambdaExpr = lambdaExpr.clone();
		newLambdaExpr.setEnclosingParameters(false);
		return tryReplace(lambdaExpr, newLambdaExpr);
	}
}
