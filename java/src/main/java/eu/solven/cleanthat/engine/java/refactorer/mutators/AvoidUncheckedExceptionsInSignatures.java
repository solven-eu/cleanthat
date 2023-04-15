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
import com.github.javaparser.ast.nodeTypes.NodeWithThrownExceptions;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ResolvedTypeHelpers;

/**
 * Turns `throws RuntimeException` into ``
 *
 * @author Benoit Lacelle
 */
public class AvoidUncheckedExceptionsInSignatures extends AJavaparserMutator {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of();
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
			Optional<ResolvedType> optResolved = ResolvedTypeHelpers.optResolvedType(t);

			if (optResolved.isEmpty()) {
				return false;
			}

			if (ResolvedTypeHelpers.isAssignableBy(RuntimeException.class.getName(), optResolved.get())) {
				return true;
			} else {
				return false;
			}
		});
	}

}
