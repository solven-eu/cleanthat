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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IRuleExternalUrls;

/**
 * Order modifiers according the the Java specification.
 *
 * @author Benoit Lacelle
 */
public class ModifierOrder extends AJavaParserMutator implements IMutator, IRuleExternalUrls {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModifierOrder.class);

	private static final List<String> ORDERED_MODIFIERS = ImmutableList.of("public",
			"protected",
			"private",
			"abstract",
			"default",
			"static",
			"final",
			"transient",
			"volatile",
			"synchronized",
			"native",
			"strictfp");

	@Override
	public String getId() {
		// Same name as checkstyle
		return "ModifierOrder";
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public String checkstyleUrl() {
		return "https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/modifier/ModifierOrderCheck.html";
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/reorder-modifiers.html";
	}

	@Override
	protected boolean processNotRecursively(Node node) {
		if (node instanceof NodeWithModifiers<?>) {
			NodeWithModifiers<?> nodeWithModifiers = (NodeWithModifiers<?>) node;
			NodeList<Modifier> modifiers = nodeWithModifiers.getModifiers();

			NodeList<Modifier> mutableModifiers = new NodeList<>(modifiers);

			Collections.sort(mutableModifiers, new Comparator<Modifier>() {

				@Override
				public int compare(Modifier o1, Modifier o2) {
					return compare2(o1.getKeyword().asString(), o1.getKeyword().asString());
				}

				private int compare2(String left, String right) {
					return Integer.compare(ORDERED_MODIFIERS.indexOf(left), ORDERED_MODIFIERS.indexOf(right));
				}
			});

			boolean changed = areSameReferences(modifiers, mutableModifiers);

			if (changed) {
				LOGGER.debug("We fixed the ordering of modifiers");
				nodeWithModifiers.setModifiers(mutableModifiers);
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("PMD.CompareObjectsWithEquals")
	private boolean areSameReferences(NodeList<Modifier> modifiers, NodeList<Modifier> mutableModifiers) {
		boolean changed = false;
		for (int i = 0; i < modifiers.size(); i++) {
			// Check by reference
			if (modifiers.get(i) != mutableModifiers.get(i)) {
				changed = true;
				break;
			}
		}
		return changed;
	}
}
