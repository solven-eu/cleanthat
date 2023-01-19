/*
 * Copyright 2023 Solven
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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserRule;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.pepper.logging.PepperLogHelper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will turn 'Arrays.asList("1", 2).stream()' into 'arrays.stream("1", 2)'
 * 
 * @author Benoit Lacelle
 *
 */
public class ArraysDotStream extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(StreamAnyMatch.class);

	private static final String METHOD_ASLIST = "asList";
	private static final String METHOD_STREAM = "stream";

	// Stream exists since 8
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public String getId() {
		return "ArraysDotStream";
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/use-arrays-stream.html";
	}

	// TODO Lack of checking for Stream type
	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof MethodCallExpr)) {
			return false;
		}
		MethodCallExpr methodCall = (MethodCallExpr) node;
		String methodCallIdentifier = methodCall.getNameAsString();
		if (!METHOD_STREAM.equals(methodCallIdentifier)) {
			return false;
		}

		Optional<Expression> optScope = methodCall.getScope();
		if (optScope.isEmpty()) {
			return false;
		}
		Expression scope = optScope.get();
		if (!(scope instanceof MethodCallExpr)) {
			return false;
		}
		MethodCallExpr scopeAsMethodCallExpr = (MethodCallExpr) scope;
		if (!METHOD_ASLIST.equals(scopeAsMethodCallExpr.getName().getIdentifier())) {
			return false;
		}

		Optional<Expression> optParentScope = scopeAsMethodCallExpr.getScope();
		if (optParentScope.isEmpty()) {
			return false;
		}
		Expression parentScope = optParentScope.get();
		if (!parentScope.isNameExpr()) {
			return false;
		}

		if (scopeAsMethodCallExpr.getArguments().size() != 1) {
			// TODO Handle this case with Stream.of(...)
			return false;
		}
		Expression filterPredicate = scopeAsMethodCallExpr.getArgument(0);

		boolean localTransformed = false;
		NodeList<Expression> replaceArguments = new NodeList<>(filterPredicate);
		Expression replacement = new MethodCallExpr(parentScope, METHOD_STREAM, replaceArguments);

		LOGGER.info("Turning {} into {}", methodCall, replacement);
		if (methodCall.replace(replacement)) {
			localTransformed = true;
		}

		if (localTransformed) {
			return true;
		} else {
			return false;
		}
	}
}