package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import org.junit.Ignore;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyBooleanExpression;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class SimplifyBooleanExpressionCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
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

	@CompareMethods
	public static class ThreeTimesNegated {
		public boolean pre(String s) {
			return !!!s.isEmpty();
		}

		public boolean post(String s) {
			return !s.isEmpty();
		}
	}

	@Ignore("This pin-points issue when processing edited nodes")
	@CompareMethods
	public static class FiveTimeNegated {
		public boolean pre(String s) {
			return !!!s.isEmpty();
		}

		public boolean post(String s) {
			return !s.isEmpty();
		}
	}

}
