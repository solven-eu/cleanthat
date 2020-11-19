package eu.solven.cleanthat.rules;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class CreateTempFilesUsingNio implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(UseIsEmptyOnCollections.class);

	@Override
	public String minimalJavaVersion() {
		// TODO Auto-generated method stub
		return IJdkVersionConstants.JDK_4;
	}

	@Override
	public boolean transform(MethodDeclaration pre) {
		CombinedTypeSolver ts = new CombinedTypeSolver();
		ts.add(new ReflectionTypeSolver());
		JavaParserFacade javaParserFacade = JavaParserFacade.get(ts);
		pre.walk(node -> {
			LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
			ResolvedMethodDeclaration test;
			if (node instanceof MethodCallExpr
					&& "createTempFile".equals(((MethodCallExpr) node).getName().getIdentifier())) {
				// boolean isStatic = false;
				// try {
				// isStatic = methodCall.resolve().isStatic();
				//
				// } catch(Exception e) {
				// return;
				// }
				Optional<Expression> optScope = ((MethodCallExpr) node).getScope();

				if (optScope.isPresent()) {
					ResolvedType type;
					Node scope = optScope.get();
					try {
						type = javaParserFacade.getType(scope);
					} catch (RuntimeException e) {
						LOGGER.debug("ARG", e);
						LOGGER.info("ARG solving type of scope: {}", scope);
						return;
						// throw new IllegalStateException("Issue on scope=" + scope, e);
					}

					if ("java.io.File".equals(type.asReferenceType().getQualifiedName())) {
						LOGGER.debug("Trouvé : {}", node.toString());
						process((MethodCallExpr) node);

					}
				}

			}
		});
		// TODO Auto-generated method stub
		return false;
	}

	private void process(MethodCallExpr methodExp) {
		List<Expression> arguments = methodExp.getArguments();
		if (arguments.size() == 2) {
		} else if (arguments.size() == 3) {
		}
	}

}
