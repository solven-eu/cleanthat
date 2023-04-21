package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ArithmeticOverFloats;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestArithmeticOverFloatsCases extends AJavaparserRefactorerCases {
	static final float f1 = 16777216.0f;
	static final float f2 = 1.0f;

	static final double d2 = 1.0f;

	public static void main(String[] args) {
		System.out.println(f1 + f2);
		System.out.println((double) (f1 + f2));
		System.out.println((double) f1 + f2);
	}

	@Override
	public IJavaparserAstMutator getTransformer() {
		return new ArithmeticOverFloats();
	}

	// pre is noncompliant; yields 1.6777216E7 not 1.6777217E7
	@CompareMethods
	public static class Nominal_sumInFloat {
		public double pre() {
			float c = f1 + f2;
			return c;
		}

		public double post() {
			float c = (float) ((double) f1 + f2);
			return c;
		}
	}

	@CompareMethods
	public static class Nominal_sumInFloat_wrappedParenthesis {
		public double pre() {
			float c = (f1 + f2);
			return c;
		}

		public double post() {
			float c = (float) ((double) f1 + f2);
			return c;
		}
	}

	@CompareMethods
	public static class Nominal_sumInFloat_Boxed {
		public double pre() {
			Float c = f1 + f2;
			return c;
		}

		public double post() {
			Float c = (float) ((double) f1 + f2);
			return c;
		}
	}

	// pre is noncompliant; yields 1.6777216E7 not 1.6777217E7
	@CompareMethods
	public static class Nominal_sumInDouble {
		public double pre() {
			double d = f1 + f2;
			return d;
		}

		public double post() {
			double d = (double) f1 + f2;
			return d;
		}
	}

	// pre is noncompliant; yields 1.6777216E7 not 1.6777217E7
	@CompareMethods
	public static class Nominal_sumIfReturn {
		public double pre() {
			return f1 + f2;
		}

		public double post() {
			return (double) f1 + f2;
		}
	}

	@UnmodifiedMethod
	public static class sumFloatAndDoubles {
		public double pre() {
			return f1 + d2;
		}
	}

	@CompareMethods
	public static class SumThenMultiply {
		public double pre() {
			return 2 * (f1 + f2);
		}

		public double post() {
			return 2 * ((double) f1 + f2);
		}
	}

	// While it is OK if just for printing (as suggested by Sonar)
	// The method may do computations base don the input float
	@UnmodifiedMethod
	@CaseNotYetImplemented
	public static class MethodArgument {
		public void pre() {
			System.out.println(Float.toString(f1 + f2));
		}

		public void post() {
			System.out.println(Float.toString((float) ((double) f1 + f2)));
		}
	}

}
