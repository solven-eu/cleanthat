package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyBooleanExpression;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestSimplifyBooleanExpressionCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new SimplifyBooleanExpression();
	}

	@CompareMethods
	public static class IfEquals {
		public void pre(int i) {
			if (!(i == 2)) {
				System.out.print(i);
			}
		}

		public void post(int i) {
			if (i != 2) {
				System.out.print(i);
			}
		}
	}

	@CompareMethods
	public static class NotLessThan {
		public boolean pre(int i) {
			return !(i < 10);
		}

		public boolean post(int i) {
			return i >= 10;
		}
	}

}
