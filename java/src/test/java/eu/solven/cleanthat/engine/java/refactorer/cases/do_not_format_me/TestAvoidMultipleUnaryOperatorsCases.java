package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import org.junit.Ignore;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidMultipleUnaryOperators;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestAvoidMultipleUnaryOperatorsCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new AvoidMultipleUnaryOperators();
	}

	@CompareMethods
	public static class NotNotBoolean {
		public boolean pre() {
			return !!true; // 2 times
		}

		public boolean post() {
			return true; // 2 times
		}
	}

	@CompareMethods
	public static class NotNotNotBoolean {
		public boolean pre() {
			return !!!true; // 3 times
		}

		public boolean post() {
			return !true; // 3 times
		}
	}

	@Ignore("This pin-points issue when processing edited nodes")
	@CompareMethods
	public static class FiveTimeNegated {
		public boolean pre(String s) {
			return !!!!!s.isEmpty();
		}

		public boolean post(String s) {
			return !!!s.isEmpty();
		}
	}

	@CompareMethods
	public static class MinusMinusInt {
		public int pre(int i) {
			return - -i;
		}

		public int post(int i) {
			return i;
		}
	}

	@CompareMethods
	public static class MinusMinusMinusInt {
		public int pre(int i) {
			return - - -i;
		}

		public int post(int i) {
			return -i;
		}
	}

	@CompareMethods
	public static class PlusPlusInt {
		public int pre(int i) {
			return + +i;
		}

		public int post(int i) {
			return i;
		}
	}

	@CompareMethods
	public static class PlusPlusPlusInt {
		public int pre(int i) {
			return + + +i;
		}

		public int post(int i) {
			return +i;
		}
	}

	@CompareMethods
	public static class BitwiseBitwiseInt {
		public int pre(int i) {
			return ~~i;
		}

		public int post(int i) {
			return i;
		}
	}

	@CompareMethods
	public static class BitwiseBitwiseBitwiseInt {
		public int pre(int i) {
			return ~~~i;
		}

		public int post(int i) {
			return ~i;
		}
	}

}
