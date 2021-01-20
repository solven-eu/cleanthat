package eu.solven.cleanthat.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Clean the way of converting primitives into {@link String}.
 *
 * @author Benoit Lacelle
 */
// https://jsparrow.github.io/rules/primitive-boxed-for-string.html
// https://rules.sonarsource.com/java/RSPEC-1158
public class PrimitiveBoxedForString extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(PrimitiveBoxedForString.class);

	@Override
	public String minimalJavaVersion() {
		return "1.1";
	}

	@Override
	public boolean transform(MethodDeclaration tree) {
		tree.walk(node -> {
			LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
			onMethodName(node, "toString", (methodNode, scope, type) -> {
				process(methodNode, scope, type);
			});
		});
		return false;
	}

	private void process(Node node, Expression scope, ResolvedType type) {
		if (!type.isReferenceType()) {
			return;
		}
		LOGGER.debug("{} is referenceType", type);
		if (Boolean.class.getName().equals(type.asReferenceType().getQualifiedName())
				|| Byte.class.getName().equals(type.asReferenceType().getQualifiedName())
				|| Short.class.getName().equals(type.asReferenceType().getQualifiedName())
				|| Integer.class.getName().equals(type.asReferenceType().getQualifiedName())
				|| Long.class.getName().equals(type.asReferenceType().getQualifiedName())
				|| Float.class.getName().equals(type.asReferenceType().getQualifiedName())
				|| Double.class.getName().equals(type.asReferenceType().getQualifiedName())) {
			LOGGER.debug("{} is AutoBoxed", type);
			if (scope instanceof ObjectCreationExpr) {
				ObjectCreationExpr creation = (ObjectCreationExpr) scope;
				NodeList<Expression> inputs = creation.getArguments();
				MethodCallExpr replacement =
						new MethodCallExpr(new NameExpr(creation.getType().getName()), "toString", inputs);
				LOGGER.info("Turning {} into {}", node, replacement);
				node.replace(replacement);
			}
		}
	}
}
