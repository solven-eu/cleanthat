package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Arrays;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.meta.CompositeJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ModifierOrder;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryModifier;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

// https://github.com/solven-eu/cleanthat/issues/802
public class TestModifierOrderThenUnnecessaryModifierCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new CompositeJavaparserMutator(Arrays.asList(
				 new ModifierOrder(), 
				new UnnecessaryModifier()));
	}

	// https://github.com/solven-eu/cleanthat/issues/802
	@CompareCompilationUnitsAsStrings(pre = "interface TopLevelInterface { public  final static int i = 0; }",
			post = "interface TopLevelInterface { int i = 0; }")
	public static class Issue802 {
	}

}
