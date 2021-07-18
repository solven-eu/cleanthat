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
		ts.add(new ReflectionTypeSolver());
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
		throw new UnsupportedOperationException("TODO Implement me in overriden classes");
	}

	protected Optional<ResolvedType> optResolvedType(Expression scope) {
		try {
			return Optional.of(getThreadJavaParser().getType(scope));
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
				consumer.onMethodName(node, scope, type.get());
			}
		}
	}
}
