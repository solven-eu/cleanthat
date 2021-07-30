package eu.solven.cleanthat.rules;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import eu.solven.cleanthat.rules.function.OnMethodName;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.meta.IRuleExternalUrls;

/**
 * Enables common behavior to JavaParser-based rules
 *
 * @author Benoit Lacelle
 */
public abstract class AJavaParserRule implements IClassTransformer, IRuleExternalUrls {

	private static final Logger LOGGER = LoggerFactory.getLogger(AJavaParserRule.class);

	private static final ThreadLocal<JavaParserFacade> TL_JAVAPARSER = ThreadLocal.withInitial(() -> {
		CombinedTypeSolver ts = new CombinedTypeSolver();

		// We allow processing any types available to default classLoader
		boolean jreOnly = false;

		ts.add(new ReflectionTypeSolver(jreOnly));
		return JavaParserFacade.get(ts);
	});

	protected final JavaParserFacade getThreadJavaParser() {
		return TL_JAVAPARSER.get();
	}

	@Override
	public boolean walkNode(Node tree) {
		AtomicBoolean transformed = new AtomicBoolean();
		tree.walk(node -> {
			if (processNotRecursively(node)) {
				transformed.set(true);
			}
		});
		return transformed.get();
	}

	protected boolean processNotRecursively(Node node) {
		Optional<Node> optReplacement = replaceNode(node);

		if (optReplacement.isPresent()) {
			Node replacement = optReplacement.get();
			return tryReplace(node, replacement);
		} else {
			return false;
		}
	}

	public boolean tryReplace(Node node, Node replacement) {
		LOGGER.info("Turning {} into {}", node, replacement);

		return node.replace(replacement);
	}

	protected Optional<Node> replaceNode(Node node) {
		throw new UnsupportedOperationException("TODO Implement me in overriden classes");
	}

	protected Optional<ResolvedType> optResolvedType(Expression scope) {
		ResolvedType calculateResolvedType;
		try {
			calculateResolvedType = scope.calculateResolvedType();
		} catch (RuntimeException e) {
			// TODO Is this related to code-modifications?
			try {
				return Optional.of(getThreadJavaParser().getType(scope));
			} catch (RuntimeException ee) {
				LOGGER.debug("Issue with JavaParser: {} {}", ee.getClass().getName(), ee.getMessage());
				return Optional.empty();
			}
		}
		try {
			ResolvedType manualResolvedType = getThreadJavaParser().getType(scope);

			if (!manualResolvedType.toString().equals(calculateResolvedType.toString())) {
				throw new IllegalStateException(
						manualResolvedType.toString() + " not equals to " + calculateResolvedType.toString());
			}
			// return Optional.of(calculateResolvedType);
			return Optional.of(manualResolvedType);
		} catch (RuntimeException e) {
			// This will happens often as, as of 2021-01, we solve types only given current class context,
			// neither other classes of the project, nor its maven/gradle dependencies
			// UnsolvedSymbolException
			// https://github.com/javaparser/javaparser/issues/1491
			LOGGER.debug("Issue with JavaParser: {} {}", e.getClass().getName(), e.getMessage());
			return Optional.empty();
		}
	}

	protected void onMethodName(Node node, String methodName, OnMethodName consumer) {
		if (node instanceof MethodCallExpr && methodName.equals(((MethodCallExpr) node).getName().getIdentifier())) {
			MethodCallExpr methodCall = (MethodCallExpr) node;
			Optional<Expression> optScope = methodCall.getScope();
			if (optScope.isEmpty()) {
				// TODO Document when this would happen
				return;
			}
			Expression scope = optScope.get();
			Optional<ResolvedType> type = optResolvedType(scope);
			if (type.isPresent()) {
				consumer.onMethodName(methodCall, scope, type.get());
			}
		}
	}
}
