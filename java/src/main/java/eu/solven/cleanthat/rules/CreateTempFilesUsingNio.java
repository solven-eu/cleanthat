package eu.solven.cleanthat.rules;


import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * cases inspired from https://jsparrow.github.io/rules/create-temp-files-using-java-nio.html#description
 *
 * @author Sébastien Collard
 */

public class CreateTempFilesUsingNio implements IClassTransformer{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UseIsEmptyOnCollections.class);

	@Override
	public String minimalJavaVersion() {
		// TODO Auto-generated method stub
		return IJdkVersionConstants.JDK_4;
	}


	@Override
	public boolean transform(MethodDeclaration pre) {
		pre.walk(node -> {
			LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
			ResolvedMethodDeclaration test;
			if (node instanceof MethodCallExpr && "createTempFile".equals(((MethodCallExpr) node).getName().getIdentifier()))
			{
//				boolean isStatic = false;
//				try {
//					isStatic = methodCall.resolve().isStatic();
//					
//				} catch(Exception e) {
//					return;
//				}
				Optional<Expression> optScope = ((MethodCallExpr)node).getScope();
				if(optScope.isPresent() && "File".equals(optScope.get().toString()))
				{
					LOGGER.debug("Trouvé : {}", node.toString());
					process((MethodCallExpr)node);
				}

			}
		});
		// TODO Auto-generated method stub
		return false;
	}
	
	private void process(MethodCallExpr methodExp) {
		List<Expression> arguments = methodExp.getArguments();
		if(arguments.size() == 2)
		{
			methodExp.tryAddImportToParentCompilationUnit(List.class);
			LOGGER.debug("Add java.nio.file.Files to import");
			
			
		}
		else if(arguments.size() == 3)
		{}
	}

}
