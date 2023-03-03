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

import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithThrownExceptions;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Turns `throws RuntimeException` into ``
 *
 * @author Benoit Lacelle
 */
public class AvoidUncheckedExceptionsInSignatures extends AJavaparserMutator {

	// Object -> Throwable -> Exception -> RuntimeException
	private static final int INDEXOF_RUNTIMEEXCEPTION = 4;

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("AvoidUncheckedExceptionsInSignatures");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_design.html#avoiduncheckedexceptionsinsignatures";
	}

	@Override
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof NodeWithThrownExceptions<?>)) {
			return false;
		}
		NodeWithThrownExceptions<?> nodeWithThrown = (NodeWithThrownExceptions<?>) node;

		return nodeWithThrown.getThrownExceptions().removeIf(t -> {
			Optional<ResolvedType> optResolved = optResolvedType(t);

			if (optResolved.isEmpty() || !optResolved.get().isReferenceType()) {
				return false;
			}

			var referenceType = optResolved.get().asReferenceType();

			List<ResolvedReferenceType> ancestors = referenceType.getAllClassesAncestors();

			// https://github.com/javaparser/javaparser/issues/3929#issuecomment-1447720743
			// optResolved.get().isAssignableBy(ReferenceTypeImpl.);

			// new ReferenceTypeImpl(null)

			if (referenceType.getQualifiedName().equals(RuntimeException.class.getName())) {
				return true;
			} else if (ancestors.size() >= INDEXOF_RUNTIMEEXCEPTION
					&& ancestors.get(ancestors.size() - INDEXOF_RUNTIMEEXCEPTION)
							.getQualifiedName()
							.equals(RuntimeException.class.getName())) {
				return true;
			} else {
				return false;
			}

			// ObjectCreationExpr expression = new ObjectCreationExpr(null,
			// new ClassOrInterfaceType(RuntimeException.class.getSimpleName()),
			// new NodeList<>());
			// node.getSymbolResolver().calculateType(expression);

		});
	}

}
