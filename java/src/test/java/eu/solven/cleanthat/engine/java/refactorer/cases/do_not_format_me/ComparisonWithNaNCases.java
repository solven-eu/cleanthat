package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ComparisonWithNaN;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class ComparisonWithNaNCases extends ARefactorerCases {
	@Override
	public IMutator getTransformer() {
		return new ComparisonWithNaN();
	}

	@CompareMethods
	public static class CaseDouble {
		public Object pre(double d) {
			return d == Double.NaN;
		}

		public Object post(double d) {
			return Double.isNaN(d);
		}
	}

	@CompareMethods
	public static class CaseDouble_NanAtLeft {
		public Object pre(double d) {
			return Double.NaN == d;
		}

		public Object post(double d) {
			return Double.isNaN(d);
		}
	}

	@CompareMethods
	public static class CaseDouble_AutoBoxed {
		public Object pre(Double d) {
			return d == Double.NaN;
		}

		public Object post(Double d) {
			return d != null && Double.isNaN(d);
		}
	}

	@CompareMethods
	public static class CaseFloat {
		public Object pre(float f) {
			return f == Float.NaN;
		}

		public Object post(float f) {
			return Float.isNaN(f);
		}
	}

	@CompareMethods
	public static class CaseFloat_AutoBoxed {
		public Object pre(Float d) {
			return d == Float.NaN;
		}

		public Object post(Float d) {
			return d != null && Float.isNaN(d);
		}
	}

	@CompareMethods
	public static class CaseDouble_FloatNaN {
		public Object pre(double d) {
			return d == Float.NaN;
		}

		public Object post(double d) {
			return Double.isNaN(d);
		}
	}

	@CompareMethods
	public static class CaseFloat_DoubleNaN {
		public Object pre(float f) {
			return f == Double.NaN;
		}

		public Object post(float f) {
			return Float.isNaN(f);
		}
	}

}
