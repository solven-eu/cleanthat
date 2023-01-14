package eu.solven.cleanthat.language.java.refactorer.mutators;

import java.util.Optional;

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

import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.refactorer.AJavaParserRule;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Turns 'boolean b = (x > 1 ) ? true : callback.doIt() || true' into 'if (x > 1) { ... } else { ...}'
 * 
 * @author Benoit Lacelle
 *
 */
public class AvoidInlineConditionals extends AJavaParserRule implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(AvoidInlineConditionals.class);

	private static final String METHOD_ASLIST = "asList";
	private static final String METHOD_STREAM = "stream";

	// Stream exists since 8
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
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
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof ConditionalExpr)) {
			return false;
		}
		ConditionalExpr ternary = (ConditionalExpr) node;

		if (!ternary.getParentNode().isPresent()) {
			return false;
		}
		Node parent = ternary.getParentNode().get();

		Expression condition = ternary.getCondition();
		Expression thenExpr = ternary.getThenExpr();
		Expression elseExpr = ternary.getElseExpr();

		// Try discard a redundant blockStatement as 'if (...)' always implies it
		while (condition instanceof EnclosedExpr) {
			condition = ((EnclosedExpr) condition).getInner();
		}

		if (parent instanceof VariableDeclarator) {
			if (!parent.getParentNode().isPresent()) {
				return false;
			}
			Node grandParent = parent.getParentNode().get();
			if (!(grandParent instanceof VariableDeclarationExpr)) {
				return false;
			}
			VariableDeclarationExpr variableDeclExpr = (VariableDeclarationExpr) grandParent;
			if (variableDeclExpr.getVariables().size() != 1) {
				return false;
			}
			VariableDeclarator variableDeclarator = variableDeclExpr.getVariables().get(0);

			SimpleName variableName = variableDeclarator.getName();
			Node newNode =
					new IfStmt(condition, wrapThenElse(thenExpr, variableName), wrapThenElse(elseExpr, variableName));

			if (grandParent.getParentNode().isEmpty()) {
				return false;
			}
			Node grandGrandParent = grandParent.getParentNode().get();
			if (!(grandGrandParent instanceof ExpressionStmt)) {
				return false;
			}

			if (grandGrandParent.getParentNode().isEmpty()) {
				return false;
			}
			Node grandGrandGrandParent = grandGrandParent.getParentNode().get();
			if (!(grandGrandGrandParent instanceof BlockStmt)) {
				return false;
			}
			BlockStmt grandGrandGrandParentBlockStmt = (BlockStmt) grandGrandGrandParent;

			// We declare the variable before the 'if (...)' statement
			{
				VariableDeclarator newVariableDeclarator =
						new VariableDeclarator(variableDeclarator.getType(), variableName);
				int indexOfVariableInParent = grandGrandGrandParentBlockStmt.getStatements().indexOf(grandGrandParent);
				if (indexOfVariableInParent < 0) {
					LOGGER.error("AssertionFailed around: {}", grandGrandGrandParentBlockStmt);
					return false;
				}
				grandGrandGrandParentBlockStmt.addStatement(indexOfVariableInParent,
						new VariableDeclarationExpr(newVariableDeclarator));
			}

			return grandGrandParent.replace(newNode);
		} else if (parent instanceof ReturnStmt) {
			// https://github.com/javaparser/javaparser/issues/3850
			Node newNode = new IfStmt(condition,
					new BlockStmt(new NodeList<>(new ReturnStmt(thenExpr))),
					new BlockStmt(new NodeList<>(new ReturnStmt(elseExpr))));
			return parent.replace(newNode);
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