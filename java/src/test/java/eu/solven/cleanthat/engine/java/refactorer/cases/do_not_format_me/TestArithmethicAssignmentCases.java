package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ArithmethicAssignment;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestArithmethicAssignmentCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserAstMutator getTransformer() {
		return new ArithmethicAssignment();
	}

	@CompareMethods
	public static class PlusInt {
		public double pre(int add) {
			int i = 3;
			i = i + add;
			return i;
		}

		public double post(int add) {
			int i = 3;
			i += add;
			return i;
		}
	}

	@CompareMethods
	public static class PlusInt_expression {
		public double pre(int add) {
			int i = 3;
			i = i + (add + i);
			return i;
		}

		public double post(int add) {
			int i = 3;
			i += (add + i);
			return i;
		}
	}

	@CompareMethods
	public static class MinusLong {
		public double pre(int add) {
			long i = 3;
			i = i - add;
			return i;
		}

		public double post(int add) {
			long i = 3;
			i -= add;
			return i;
		}
	}

	@CompareMethods
	public static class MultiplyFloat_variableOnRight {
		public double pre(float factor) {
			float i = 3;
			i = factor * i;
			return i;
		}

		public double post(float factor) {
			float i = 3;
			i *= factor;
			return i;
		}
	}

	@CompareMethods
	public static class DivisionDouble {
		public double pre(double factor) {
			double i = 3;
			i = i / factor;
			return i;
		}

		public double post(double factor) {
			double i = 3;
			i /= factor;
			return i;
		}
	}

	@UnmodifiedMethod
	public static class DivisionDouble_variableOnRight {
		public double pre(double factor) {
			double i = 3;
			i = factor / i;
			return i;
		}
	}

	@CompareMethods
	public static class PlusCharArgument {
		public double pre(char add) {
			int i = 3;
			i = i + add;
			return i;
		}

		public double post(char add) {
			int i = 3;
			i += add;
			return i;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class PlusShortVariable {
		public double pre(short add) {
			short i = 3;
			i = (short) (i + add);
			return i;
		}

		public double post(short add) {
			short i = 3;
			i += add;
			return i;
		}
	}

	@CompareMethods
	public static class PlusString_suffix {
		public String pre(String add) {
			String i = "initial";
			i = i + add;
			return i;
		}

		public String post(String add) {
			String i = "initial";
			i += add;
			return i;
		}
	}

	@UnmodifiedMethod
	public static class PlusString_prefix {
		public String pre(String add) {
			String i = "initial";
			i = add + i;
			return i;
		}
	}

	@CompareMethods
	public static class PlusString_suffix_object {
		public String pre(Object add) {
			String i = "initial";
			i = i + add;
			return i;
		}

		public String post(Object add) {
			String i = "initial";
			i += add;
			return i;
		}
	}

	@UnmodifiedMethod
	public static class PlusString_prefix_object {
		public String pre(Object add) {
			String i = "initial";
			i = add + i;
			return i;
		}
	}

	@CompareMethods
	public static class PlusString_suffix_int {
		public String pre(int add) {
			String i = "initial";
			i = i + add;
			return i;
		}

		public String post(int add) {
			String i = "initial";
			i += add;
			return i;
		}
	}

	@UnmodifiedMethod
	public static class PlusString_prefix_int {
		public String pre(int add) {
			String i = "initial";
			i = add + i;
			return i;
		}

	}
}
