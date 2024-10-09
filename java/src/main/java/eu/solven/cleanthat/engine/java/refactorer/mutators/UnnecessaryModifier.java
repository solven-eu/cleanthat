/*
 * Copyright 2023-2024 Benoit Lacelle - SOLVEN
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
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
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndSymbolSolver) {
		Node node = nodeAndSymbolSolver.getNode();
		if (!(node instanceof Modifier)) {
			return false;
		}
		var modifier = (Modifier) node;

		if (modifier.getParentNode().isEmpty()) {
			return false;
		}

		var parentNode = modifier.getParentNode().get();
		if (!(parentNode instanceof MethodDeclaration) && !(parentNode instanceof FieldDeclaration)
		// TypeDeclaration covers interface|class|enum|record
				&& !(parentNode instanceof TypeDeclaration)
				&& !(parentNode instanceof AnnotationMemberDeclaration)) {
			return false;
		}

		var isNestedOrInnerClass = (parentNode instanceof ClassOrInterfaceDeclaration
				&& !((ClassOrInterfaceDeclaration) parentNode).isInterface());

		if (parentNode instanceof RecordDeclaration) {
			// Records are implicitly final, independently of their parentNode
			if (modifier.getKeyword() == Keyword.FINAL) {
				return removeModifier(modifier);
			}

			// Records are implicitly static, independently of their parentNode
			// https://github.com/projectlombok/lombok/issues/3140
			if (modifier.getKeyword() == Keyword.STATIC) {
				return removeModifier(modifier);
			}
		} else if (parentNode instanceof MethodDeclaration) {
			// static methods is never implicit
			if (modifier.getKeyword() == Keyword.STATIC) {
				return false;
			}
		}

		if (parentNode.getParentNode().isEmpty()) {
			// Given a modifier, the grandParent node would be the owning method|field|type
			return false;
		}
		var grandParentNode = parentNode.getParentNode().get();

		if (!(grandParentNode instanceof TypeDeclaration)) {
			// Only types like Interfaces and Annotations may have fields|methods with implicit modifiers
			return false;
		}

		var isPublic = modifier.getKeyword() == Keyword.PUBLIC;
		var isAbstract = modifier.getKeyword() == Keyword.ABSTRACT;
		var isFinal = modifier.getKeyword() == Keyword.FINAL;
		var isStatic = modifier.getKeyword() == Keyword.STATIC;

		boolean isInInterfaceLike = isInterfaceLike(grandParentNode);

		if (!isInInterfaceLike) {
			return false;
		}

		// We are considering a modifier from an interface method|field|classOrInterface|annotation|enum
		if (isPublic || isAbstract && !isNestedOrInnerClass || isFinal && !isNestedOrInnerClass || isStatic) {
			return removeModifier(modifier);
		}

		return false;
	}

	private boolean isInterfaceLike(Node node) {
		if (node instanceof AnnotationDeclaration) {
			// Annotations (like `@SomeAnnotation`) behave like interfaces regarding modifiers
			return true;
		} else if (node instanceof ClassOrInterfaceDeclaration) {
			// A plain interface
			return ((ClassOrInterfaceDeclaration) node).isInterface();
		}

		return false;
	}

	/**
	 * We return a boolean, like {@link Modifier#remove()}
	 * 
	 * @param modifier
	 * @return always true. If false, it would throw.
	 */
	private boolean removeModifier(Modifier modifier) {
		// https://github.com/javaparser/javaparser/issues/3935
		NodeWithModifiers<?> nodeWithModifiers = (NodeWithModifiers<?>) modifier.getParentNode().get();

		// Do not rely on a `NodeList` to prevent transfer of modifiers ownership
		// https://github.com/solven-eu/cleanthat/issues/802
		List<Modifier> mutableModifiers = new ArrayList<>(nodeWithModifiers.getModifiers());

		// Remove from the plainList: it won't change the AST (yet)
		if (!mutableModifiers.remove(modifier)) {
			throw new IllegalStateException("Issue removing " + modifier + " from " + mutableModifiers);
		}

		// https://github.com/javaparser/javaparser/issues/3935
		nodeWithModifiers.setModifiers();

		NodeList<Modifier> asNodeList = new NodeList<>(mutableModifiers);
		nodeWithModifiers.setModifiers(asNodeList);

		return true;
	}
}
