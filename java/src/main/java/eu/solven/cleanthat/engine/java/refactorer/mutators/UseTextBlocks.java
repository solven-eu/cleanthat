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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserMutator;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Turns '"a\r\n" + "b\r\n"’ into ’"""aEOLbEOL"""'
 *
 * @author Benoit Lacelle
 */
// https://www.baeldung.com/java-text-blocks
// https://stackoverflow.com/questions/878573/does-java-have-support-for-multiline-strings/50155171#50155171
// TODO Handle intermediate parenthesis
public class UseTextBlocks extends AJavaParserMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseTextBlocks.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_15;
	}

	@Override
	public Optional<String> getCleanthatId() {
		return Optional.of("UseTextBlocks");
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof Expression)) {
			return false;
		}

		Expression rootExp = (Expression) node;

		Optional<List<StringLiteralExpr>> optAsList = optAsList(rootExp);

		if (!optAsList.isPresent()) {
			return false;
		}

		List<StringLiteralExpr> listStringExpr = optAsList.get();

		String concat = listStringExpr.stream().map(s -> s.asString()).collect(Collectors.joining());

		// TextBlocks are '\n'-based
		List<String> rows = Arrays.asList(concat.split("\n"));

		if (rows.size() <= 1) {
			return false;
		}

		Node textBlock = new TextBlockLiteralExpr(concat);

		return node.replace(textBlock);
	}

	private Optional<List<StringLiteralExpr>> optAsList(Expression expr) {
		if (expr.isStringLiteralExpr()) {
			return Optional.of(Collections.singletonList(expr.asStringLiteralExpr()));
		} else if (expr.isIntegerLiteralExpr()) {
			IntegerLiteralExpr asIntExpr = expr.asIntegerLiteralExpr();
			String intAsString = Integer.toString(asIntExpr.asNumber().intValue());
			return Optional.of(Collections.singletonList(new StringLiteralExpr(intAsString)));
		} else if (!expr.isBinaryExpr()) {
			return Optional.empty();
		}

		BinaryExpr binaryExpr = expr.asBinaryExpr();
		if (binaryExpr.getOperator() != Operator.PLUS) {
			return Optional.empty();
		}

		List<StringLiteralExpr> underlyingStrings = new ArrayList<>();

		Expression left = binaryExpr.getLeft();
		Optional<List<StringLiteralExpr>> leftAsList = optAsList(left);
		if (leftAsList.isEmpty()) {
			return Optional.empty();
		} else {
			underlyingStrings.addAll(leftAsList.get());
		}

		Expression right = binaryExpr.getRight();
		Optional<List<StringLiteralExpr>> rightAsList = optAsList(right);
		if (rightAsList.isEmpty()) {
			return Optional.empty();
		} else {
			underlyingStrings.addAll(rightAsList.get());
		}

		return Optional.of(underlyingStrings);
	}

}
