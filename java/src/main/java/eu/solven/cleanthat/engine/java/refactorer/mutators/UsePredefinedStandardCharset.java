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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `Charset.forName("UTF-8")` into `StandardCharsets.UTF_8`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe({ OptionalWrappedIfToFilter.class, OptionalWrappedVariableToMap.class })
public class UsePredefinedStandardCharset extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_7;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("NIO");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RPSEC-4719");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("UsePredefinedStandardCharset");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/use-predefined-standard-charset.html";
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isMethodCallExpr()) {
			return false;
		}

		MethodCallExpr methodCallExpr = expr.asMethodCallExpr();

		if (!"forName".equals(methodCallExpr.getNameAsString())) {
			return false;
		} else if (methodCallExpr.getArguments().size() != 1) {
			return false;
		} else if (!scopeHasRequiredType(methodCallExpr.getScope(), Charset.class)) {
			return false;
		}

		Expression singleArgument = methodCallExpr.getArgument(0);

		Optional<String> optFieldName = findCharset(singleArgument);

		if (optFieldName.isEmpty()) {
			return false;
		}

		NameExpr className =
				new NameExpr(nameOrQualifiedName(expr.findCompilationUnit().get(), StandardCharsets.class));
		return tryReplace(methodCallExpr, new FieldAccessExpr(className, optFieldName.get()));

	}

	private Optional<String> findCharset(Expression singleArgument) {
		if (new StringLiteralExpr("UTF-8").equals(singleArgument)) {
			return Optional.of("UTF_8");
		} else if (new StringLiteralExpr("UTF-16").equals(singleArgument)) {
			return Optional.of("UTF_16");
		} else if (new StringLiteralExpr("UTF-16BE").equals(singleArgument)) {
			return Optional.of("UTF_16BE");
		} else if (new StringLiteralExpr("UTF-16LE").equals(singleArgument)) {
			return Optional.of("UTF_16LE");
		} else if (new StringLiteralExpr("US-ASCII").equals(singleArgument)) {
			return Optional.of("US_ASCII");
		} else if (new StringLiteralExpr("ISO-8859-1").equals(singleArgument)) {
			return Optional.of("ISO_8859_1");
		}
		return Optional.empty();
	}

}
