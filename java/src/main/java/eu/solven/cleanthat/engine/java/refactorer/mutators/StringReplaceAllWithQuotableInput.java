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

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns `s.replaceAll("abc", "")` into `s.replace("abc", "")`
 *
 * @author Benoit Lacelle
 */
@Slf4j
public class StringReplaceAllWithQuotableInput extends AJavaparserExprMutator {
	// https://stackoverflow.com/questions/5105143/list-of-all-characters-that-should-be-escaped-before-put-in-to-regex
	private static final String IS_REGEX_SPECIAL_CHAR = ".\\+*?[^]$(){}=!<>|:-";
	private static final String IS_NOT_REGEX = IS_REGEX_SPECIAL_CHAR.chars()
			.mapToObj(c -> "\\" + Character.toString((char) c))
			.collect(Collectors.joining("", "[^", "]"));

	// isQuote if a wordCharacter, or a space, or a non-regex-semantic character, or the escape of a non-word character.
	// A backslash followed by a word character is either a known escaped construct, or a potentially future one (i.e.
	// it is a reserved syntax) (see backslash section in
	// https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html)
	// We consider as simple quote if:
	// we have plain characters, or space, or dash, or underscore
	// Or a '\' (which is escaped (x2) in the input (Java text), escaped again in the sourceCode regex (x4), escaped
	// again in our matching regex (x8))
	// We add a `|_|` as `_` is excluded by `[^\\w]` as it is considered a word-character
	private static final String IS_ESCAPED_REGEX_CHAR = "(?:\\\\\\\\(?:[^\\w]|_|\\\\\\\\))";

	private static final String IS_QUOTE_REGEX = "(?:" + IS_NOT_REGEX + "|" + IS_ESCAPED_REGEX_CHAR + ")*";

	private static final Pattern IS_QUOTE = Pattern.compile(IS_QUOTE_REGEX);

	static {
		LOGGER.debug("Regex matching quotable regex: {}", IS_QUOTE_REGEX);
	}

	@Override
	public String minimalJavaVersion() {
		// `replaceAll` has been introduced with JDK4
		return IJdkVersionConstants.JDK_4;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-5361");
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isMethodCallExpr()) {
			return false;
		}
		var methodCall = expr.getNode().asMethodCallExpr();

		if (!"replaceAll".equals(methodCall.getNameAsString())) {
			return false;
		} else if (methodCall.getArguments().size() != 2) {
			return false;
		} else if (!MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(methodCall.getScope()), String.class)) {
			return false;
		} else if (!MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(methodCall.getArgument(0)),
				String.class)) {
			return false;
		} else if (!methodCall.getArgument(0).isLiteralStringValueExpr()) {
			return false;
		}

		LiteralStringValueExpr literalRegex = methodCall.getArgument(0).asLiteralStringValueExpr();
		String regex = literalRegex.getValue();

		if (!isQuotableRegex(regex)) {
			// The regex is not a simple quote
			return false;
		}

		// Escape character can be removed as we will replace as a quote
		var regexAsQuote = regex.replaceAll("(?:\\\\\\\\([^\\w]|\\\\\\\\))", "$1");

		literalRegex.setValue(regexAsQuote);
		methodCall.setName("replace");

		return true;
	}

	public static boolean isQuotableRegex(String regex) {
		return IS_QUOTE.matcher(regex).matches();
	}
}
