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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Turns 'boolean b = (x > 1 ) ? true : callback.doIt() || true' into 'if (x > 1) { ... } else { ...}'
 * 
 * @author Benoit Lacelle
 *
 */
public class AvoidInlineConditionals extends AJavaparserNodeMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(AvoidInlineConditionals.class);

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of();
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-3358");
	}

	@Override
	public Optional<String> getCheckstyleId() {
		return Optional.of("AvoidInlineConditionals");
	}

	@Override
	public String checkstyleUrl() {
		return "https://checkstyle.sourceforge.io/config_coding.html#AvoidInlineConditionals";
	}

	// TODO Lack of checking for Stream type
	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof ConditionalExpr)) {
			return false;
		}
		var ternary = (ConditionalExpr) node.getNode();

		if (ternary.getParentNode().isEmpty()) {
			return false;
		}
		var parent = ternary.getParentNode().get();

		var condition = ternary.getCondition();

		// Try discard a redundant blockStatement as 'if (...)' always implies it
		while (condition instanceof EnclosedExpr) {
			condition = ((EnclosedExpr) condition).getInner();
		}

		if (parent instanceof VariableDeclarator) {
			if (parent.getParentNode().isEmpty()) {
				return false;
			}
			var grandParent = parent.getParentNode().get();
			if (!(grandParent instanceof VariableDeclarationExpr)) {
				return false;
			}

			if (grandParent.getParentNode().isEmpty()) {
				return false;
			}
			var grandGrandParent = grandParent.getParentNode().get();
			if (!(grandGrandParent instanceof ExpressionStmt)) {
				return false;
			}

			if (grandGrandParent.getParentNode().isEmpty()) {
				return false;
			}
			var grandGrandGrandParent = grandGrandParent.getParentNode().get();
			if (!(grandGrandGrandParent instanceof BlockStmt)) {
				return false;
			}

			var variableDeclExpr = (VariableDeclarationExpr) grandParent;
			if (variableDeclExpr.getVariables().size() != 1) {
				return false;
			} else if (variableDeclExpr.getElementType().isVarType()) {
				// We can not have a `var` variable with no initializer
				return false;
			}
			var variableDeclarator = variableDeclExpr.getVariables().get(0);

			var variableName = variableDeclarator.getName();
			var grandGrandGrandParentBlockStmt = (BlockStmt) grandGrandGrandParent;

			// We declare the variable before the 'if (...)' statement
			{
				var indexOfVariableInParent = grandGrandGrandParentBlockStmt.getStatements().indexOf(grandGrandParent);
				if (indexOfVariableInParent < 0) {
					LOGGER.error("Issue searching for {} inside {}", grandGrandParent, grandGrandGrandParentBlockStmt);
					return false;
				}

				var newVariableDeclarator = new VariableDeclarator(variableDeclarator.getType(), variableName);
				grandGrandGrandParentBlockStmt.addStatement(indexOfVariableInParent,
						new VariableDeclarationExpr(newVariableDeclarator));
			}

			var thenExpr = ternary.getThenExpr();
			var elseExpr = ternary.getElseExpr();
			Node newNode =
					new IfStmt(condition, wrapThenElse(thenExpr, variableName), wrapThenElse(elseExpr, variableName));

			return tryReplace(grandGrandParent, newNode);
		} else if (parent instanceof ReturnStmt) {
			var thenExpr = ternary.getThenExpr();
			var elseExpr = ternary.getElseExpr();

			// https://github.com/javaparser/javaparser/issues/3850
			Node newNode = new IfStmt(condition,
					new BlockStmt(new NodeList<>(new ReturnStmt(thenExpr))),
					new BlockStmt(new NodeList<>(new ReturnStmt(elseExpr))));
			return tryReplace(parent, newNode);
			// return false;
		} else {
			return false;
		}
	}

	private BlockStmt wrapThenElse(Expression thenExpr, SimpleName variableName) {
		return new BlockStmt(new NodeList<>(
				new ExpressionStmt(new AssignExpr(new NameExpr(variableName), thenExpr, Operator.ASSIGN))));
	}
}