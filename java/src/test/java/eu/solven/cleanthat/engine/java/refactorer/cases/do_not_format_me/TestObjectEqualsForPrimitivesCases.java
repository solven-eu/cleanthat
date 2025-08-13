package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Objects;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ObjectEqualsForPrimitives;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestObjectEqualsForPrimitivesCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new ObjectEqualsForPrimitives();
	}

	@CompareMethods
	public static class CaseInt {
		public boolean pre(int a, int b) {
			return Objects.equals(a, b);
		}

		public boolean post(int a, int b) {
			return a == b;
		}
	}

	@CompareMethods
	public static class CaseDouble {
		public boolean pre(double a, double b) {
			return Objects.equals(a, b);
		}

		public boolean post(double a, double b) {
			return a == b;
		}
	}

	// Commented until the following is fixed (NOK with Eclipse 3.36.0 (June 2025))
	// https://github.com/eclipse-jdt/eclipse.jdt.core/issues/3870
	// @UnmodifiedMethod
	// public static class CaseIntLong {
	// public boolean pre(int a, long b) {
	// return Objects.equals(a, b);
	// }
	// }

	// Commented until the following is fixed (NOK with Eclipse 3.36.0 (June 2025))
	// https://github.com/eclipse-jdt/eclipse.jdt.core/issues/3870
	// @UnmodifiedMethod
	// public static class CaseIntDouble {
	// public boolean pre(int a, double b) {
	// return Objects.equals(a, b);
	// }
	// }

	@UnmodifiedMethod
	public static class CaseObject {
		public boolean pre(Object a, Object b) {
			return Objects.equals(a, b);
		}
	}

	@UnmodifiedMethod
	public static class CaseIntAndObject {
		public boolean pre(int a, Object b) {
			return Objects.equals(a, b);
		}
	}
}
