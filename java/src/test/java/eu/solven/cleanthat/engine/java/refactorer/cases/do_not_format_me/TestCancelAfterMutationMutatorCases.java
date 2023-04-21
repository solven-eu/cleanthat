package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;
import java.util.Optional;

import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.corrupted.CancelAfterMutationMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestCancelAfterMutationMutatorCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new CancelAfterMutationMutator();
	}

	@UnmodifiedMethod
	public static class WithExpr {
		public Object pre(List<?> list) {
			return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
		}
	}
}