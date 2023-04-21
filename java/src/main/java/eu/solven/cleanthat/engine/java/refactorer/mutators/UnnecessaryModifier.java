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

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Turns 'public static final someMethod();' into 'someMethod();' in interfaces
 *
 * @author Benoit Lacelle
 */
public class UnnecessaryModifier extends AJavaparserNodeMutator {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("ExplicitToImplicit");
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UnnecessaryModifier");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#unnecessarymodifier";
	}

	@Override
	public Optional<String> getCheckstyleId() {
		return Optional.of("RedundantModifier");
	}

	@Override
	public String checkstyleUrl() {
		return "https://checkstyle.sourceforge.io/config_modifier.html#RedundantModifier";
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-2333");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("RemoveModifiersInInterfaceProperties");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/remove-modifiers-in-interface-properties.html";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof Modifier)) {
			return false;
		}
		var modifier = (Modifier) node.getNode();

		if (modifier.getParentNode().isEmpty()) {
			return false;
		}

		var parentNode = modifier.getParentNode().get();
		if (!(parentNode instanceof MethodDeclaration) && !(parentNode instanceof FieldDeclaration)
				&& !(parentNode instanceof ClassOrInterfaceDeclaration)
				&& !(parentNode instanceof AnnotationMemberDeclaration)) {
			return false;
		}

		if (parentNode.getParentNode().isEmpty()) {
			return false;
		}
		var grandParentNode = parentNode.getParentNode().get();
		if (grandParentNode instanceof ClassOrInterfaceDeclaration) {
			var grandParentInterface = (ClassOrInterfaceDeclaration) grandParentNode;

			if (!grandParentInterface.isInterface()) {
				return false;
			}

			// We are considering a modifier from an interface method|field|classOrInterface
			if (modifier.getKeyword() == Keyword.PUBLIC || modifier.getKeyword() == Keyword.ABSTRACT
					|| modifier.getKeyword() == Keyword.FINAL
					|| modifier.getKeyword() == Keyword.STATIC && !(parentNode instanceof MethodDeclaration)) {
				return modifier.remove();
			}

			return false;
		} else if (grandParentNode instanceof AnnotationDeclaration) {
			// We are considering a modifier from an annotation method|field|classOrInterface
			if (modifier.getKeyword() == Keyword.PUBLIC || modifier.getKeyword() == Keyword.ABSTRACT
					|| modifier.getKeyword() == Keyword.FINAL
					|| modifier.getKeyword() == Keyword.STATIC) {
				return modifier.remove();
			}

			return false;
		} else {
			return false;
		}

	}
}
