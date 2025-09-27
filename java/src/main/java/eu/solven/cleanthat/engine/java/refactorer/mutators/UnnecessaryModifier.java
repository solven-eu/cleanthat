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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns 'public static final someMethod();' into 'someMethod();' in interfaces
 *
 * @author Benoit Lacelle
 */
@Slf4j
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
		return "https://pmd.github.io/pmd/pmd_rules_java_codestyle.html#unnecessarymodifier";
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

	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndSymbolSolver) {
		return Optional.ofNullable(nodeAndSymbolSolver.getNode())
				.filter(Modifier.class::isInstance)
				.map(Modifier.class::cast)
				.map(modifier -> isRedundant(modifier) && removeModifier(modifier))
				.orElse(false);
	}

	private boolean isRedundant(Modifier modifier) {
		return modifier.getParentNode().map(parentNode -> {
			switch (modifier.getKeyword()) {
			case ABSTRACT:
				return isImplicitlyAbstract(parentNode);
			case FINAL:
				return isImplicitlyFinal(parentNode);
			case PUBLIC:
				return isImplicitlyPublic(parentNode);
			case STATIC:
				return isImplicitlyStatic(parentNode);
			default:
				return false;
			}
		}).orElse(false);
	}

	private boolean isImplicitlyAbstract(Node node) {
		return isInterfaceLike(node)
				|| (node instanceof MethodDeclaration || node instanceof AnnotationMemberDeclaration)
						&& node.getParentNode().filter(this::isInterfaceLike).isPresent();
	}

	private boolean isImplicitlyFinal(Node node) {
		return node instanceof RecordDeclaration
				|| node instanceof FieldDeclaration && node.getParentNode().filter(this::isInterfaceLike).isPresent()
				|| node instanceof MethodDeclaration && ((MethodDeclaration) node).isPrivate();
	}

	private boolean isImplicitlyPublic(Node node) {
		return node.getParentNode().filter(this::isInterfaceLike).isPresent();
	}

	private boolean isImplicitlyStatic(Node node) {
		// interfaces and annotations are implicitly static
		return isInterfaceLike(node)
				// enums are implicitly static
				// https://stackoverflow.com/questions/23127926/static-enum-vs-non-static-enum
				|| node instanceof EnumDeclaration
				// records are implicitly static
				// https://github.com/projectlombok/lombok/issues/3140
				|| node instanceof RecordDeclaration
				|| (node instanceof ClassOrInterfaceDeclaration || node instanceof FieldDeclaration)
						&& node.getParentNode().filter(this::isInterfaceLike).isPresent();
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
