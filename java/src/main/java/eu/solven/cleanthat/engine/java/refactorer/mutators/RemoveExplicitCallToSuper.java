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
import java.util.Set;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Turns 'SomeClassWithConstructor(){super(); someMethod();}` into `SomeClassWithConstructor(){someMethod();}`.
 * 
 * This will also remove references to `this()` as same class empty constructor.
 * 
 * This will not remove `super();` if it is the only statement of a constructor, as it is considered better to have
 * `super();` than an empty constructor. See `PMD.UncommentedEmptyConstructor`.
 * 
 * This mutator would contradict `PMD.CallSuperInConstructor`. See
 * https://pmd.github.io/pmd/pmd_rules_java_codestyle.html#callsuperinconstructor
 *
 * @author Benoit Lacelle
 */
public class RemoveExplicitCallToSuper extends AJavaparserNodeMutator {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1DOT1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("ExplicitToImplicit");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("RemoveExplicitCallToSuper");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/remove-explicit-call-to-super.html";
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof ConstructorDeclaration)) {
			return false;
		}

		var constructor = (ConstructorDeclaration) node.getNode();

		var body = constructor.getBody();

		if (body.getStatements().isEmpty()) {
			return false;
		}

		var firstStatement = body.getStatement(0);

		if (!firstStatement.isExplicitConstructorInvocationStmt()) {
			return false;
		}

		if (!firstStatement.asExplicitConstructorInvocationStmt().getArguments().isEmpty()) {
			return false;
		}

		if (body.getStatements().size() == 1) {
			// We keep `super();` if it is the only statement, as it is generally considered better than having an empty
			// constructor.
			return false;
		}

		return tryRemove(firstStatement);
	}
}
