package eu.solven.cleanthat.rules;

import com.github.javaparser.ast.body.MethodDeclaration;

import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class CreateTempFilesUsingNio implements IClassTransformer{

	public CreateTempFilesUsingNio() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean transform(MethodDeclaration pre) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String minimalJavaVersion() {
		// TODO Auto-generated method stub
		return null;
	}

}
