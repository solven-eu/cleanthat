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

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyMeBefore;

/**
 * Migrate from 'm.length() == 0’ to ’m.isEmpty()'. Works with {@link String}.
 *
 * @author Benoit Lacelle
 */
// We prefer turning `username.equals("")` into `username.isEmpty()` than `"".equals(username)`
@ApplyMeBefore({ LiteralsFirstInComparisons.class })
public class UseStringIsEmpty extends AUseXIsEmpty {
	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public String minimalJavaVersion() {
		// java.lang.String.isEmpty() exists since 1.6
		return IJdkVersionConstants.JDK_6;
	}

	@Override
	public Optional<String> getCleanthatId() {
		// Naming similar to UseCollectionIsEmpty
		return Optional.of("UseStringIsEmpty");
	}

	@Override
	protected String getSizeMethod() {
		return "length";
	}

	@Override
	protected Set<Class<?>> getCompatibleTypes() {
		return Set.of(String.class);
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		boolean replaced = super.processNotRecursively(expr);

		if (replaced) {
			return true;
		}

		if (!expr.isMethodCallExpr()) {
			return false;
		}

		var asMethodCall = expr.asMethodCallExpr();
		if (!"equals".equals(asMethodCall.getNameAsString())
				&& !"equalsIgnoreCase".equals(asMethodCall.getNameAsString())) {
			return false;
		}

		if (asMethodCall.getArguments().size() != 1 || !asMethodCall.getArgument(0).isLiteralStringValueExpr()
				|| !"".equals(asMethodCall.getArgument(0).asLiteralStringValueExpr().getValue())) {
			return false;
		}

		Optional<Expression> optScope = asMethodCall.getScope();
		if (!scopeHasRequiredType(optScope, String.class)) {
			return false;
		}

		// `input.equals("")` -> `input.isEmpty()`
		return tryReplace(expr, new MethodCallExpr(optScope.get(), "isEmpty"));
	}

}
