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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Turns 'SomeClassWithConstructor(){super(); someMethod();}` into `SomeClassWithConstructor(){someMethod();}`.
 * 
 * This will also remove references to `this()` as same class empty constructor.
 *
 * @author Benoit Lacelle
 */
public class RemoveExplicitCallToSuper extends AJavaparserMutator {
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
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof ConstructorDeclaration)) {
			return false;
		}

		var constructor = (ConstructorDeclaration) node;

		var body = constructor.getBody();

		if (body.getStatements().isEmpty()) {
			return false;
		}

		var firstStatement = body.getStatement(0);

		if (!firstStatement.isExplicitConstructorInvocationStmt()) {
			return false;
		}

		if (firstStatement.asExplicitConstructorInvocationStmt().getArguments().isEmpty()) {
			return tryRemove(firstStatement);
		}

		return false;
	}
}
