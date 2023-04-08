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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.collect.ImmutableSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserStmtMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.BinaryExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.LambdaExprHelpers;

/**
 * Turns `if (s != null) {...}` into `Optional.ofNullable(s).ifPresent(...)`
 *
 * @author Benoit Lacelle
 */
public class UseOptionalOfNullable extends AJavaparserStmtMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseOptionalOfNullable.class);

	final NullLiteralExpr nullLiteralExpr = new NullLiteralExpr();

	@Override
	public String minimalJavaVersion() {
		// Optional has been introduced with JDK6
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Optional");
	}

	@Override
	protected boolean processNotRecursively(Statement stmt) {
		if (!stmt.isIfStmt()) {
			return false;
		}

		IfStmt ifStmt = stmt.asIfStmt();
		Expression expr = ifStmt.getCondition();

		if (!expr.isBinaryExpr()) {
			return false;
		} else if (ifStmt.getElseStmt().isPresent()) {
			return false;
		}
		var binaryExpr = expr.asBinaryExpr();

		BinaryExpr.Operator operator = binaryExpr.getOperator();
		if (operator != BinaryExpr.Operator.NOT_EQUALS) {
			return false;
		}

		Optional<Map.Entry<NameExpr, NullLiteralExpr>> nameAndNull =
				BinaryExprHelpers.findPair(binaryExpr, e -> e.isNameExpr(), e -> e.isNullLiteralExpr());
		if (nameAndNull.isEmpty()) {
			return false;
		}

		Optional<CompilationUnit> optCompilationUnit = expr.findCompilationUnit();
		if (optCompilationUnit.isEmpty()) {
			return false;
		}
		Statement thenStmt = ifStmt.getThenStmt();

		if (!canBePushedInLambdaExpr(thenStmt)) {
			return false;
		}

		String nullableVariableName = nameAndNull.get().getKey().getNameAsString();
		SimpleName notNullVariableName = makeUnusedVariablename(thenStmt, nullableVariableName);

		// Replace the variable name through the whole lambdaExpr
		thenStmt.findAll(NameExpr.class, n -> n.getNameAsString().equals(nullableVariableName))
				.forEach(n -> n.replace(new NameExpr(notNullVariableName)));

		Optional<LambdaExpr> optLambdaExpr = LambdaExprHelpers.makeLambdaExpr(notNullVariableName, thenStmt);
		if (optLambdaExpr.isEmpty()) {
			return false;
		}

		MethodCallExpr callOfNullable =
				new MethodCallExpr(new NameExpr(nameOrQualifiedName(optCompilationUnit.get(), Optional.class)),
						"ofNullable",
						new NodeList<>(nameAndNull.get().getKey()));

		MethodCallExpr callIfPresent =
				new MethodCallExpr(callOfNullable, "ifPresent", new NodeList<>(optLambdaExpr.get()));

		return

		tryReplace(ifStmt, new ExpressionStmt(callIfPresent));

	}

	/**
	 * We need a new variable name
	 * 
	 * @param context
	 *            context of the new variable, to make sure there is no conflict with existing variables
	 * @param nameTemplate
	 * @return
	 */
	@SuppressFBWarnings("SBSC_USE_STRINGBUFFER_CONCATENATION")
	private SimpleName makeUnusedVariablename(Node context, String nameTemplate) {
		var suffixCandidate = "_";

		while (true) {
			var nameCandidate = nameTemplate + suffixCandidate;

			// TODO This can be optimized by considering only the parent MethodDeclaration
			Optional<MethodDeclaration> parentMethodDeclaration = context.findAncestor(MethodDeclaration.class);
			if (parentMethodDeclaration.isEmpty()) {
				return new SimpleName(nameCandidate);
			}

			Optional<NameExpr> optConflict = parentMethodDeclaration.get()
					.findFirst(NameExpr.class, n -> n.getNameAsString().equals(nameCandidate));
			if (optConflict.isPresent()) {
				LOGGER.debug("We can not use `{}` as it would conflict with another variable", nameCandidate);
				suffixCandidate += "_";
			} else {
				return new SimpleName(nameCandidate);
			}
		}

	}

}
