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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Turns 's.indexOf("s")’ into ’s.indexOf('s')'.
 *
 * @author Benoit Lacelle
 */
// https://jsparrow.github.io/rules/use-is-empty-on-collections.html
public class UseIndexOfChar extends AJavaParserMutator implements IMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseIndexOfChar.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public boolean isProductionReady() {
		return true;
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_performance.html#useindexofchar";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UseIndexOfChar");
	}

	@Override
	public String sonarUrl() {
		return "https://rules.sonarsource.com/java/RSPEC-1155";
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-1155");
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof StringLiteralExpr)) {
			return false;
		}
		StringLiteralExpr stringLiteralExpr = (StringLiteralExpr) node;
		String stringLiteralExprValue = stringLiteralExpr.getValue();
		if (stringLiteralExprValue.length() != 1) {
			// We consider only String with length == to .indexOf over the single char
			return false;
		}

		if (!stringLiteralExpr.getParentNode().isPresent()) {
			return false;
		}
		Node parentNode = stringLiteralExpr.getParentNode().get();
		if (!(parentNode instanceof MethodCallExpr)) {
			// We search a call for .indexOf
			return false;
		}
		MethodCallExpr parentMethodCall = (MethodCallExpr) parentNode;
		if (!"indexOf".equals(parentMethodCall.getNameAsString())) {
			// We search a call for .indexOf
			return false;
		}

		if (!scopeHasRequiredType(parentMethodCall.getScope(), String.class)) {
			return false;
		}

		return node.replace(new CharLiteralExpr(stringLiteralExprValue.charAt(0)));
	}
}
