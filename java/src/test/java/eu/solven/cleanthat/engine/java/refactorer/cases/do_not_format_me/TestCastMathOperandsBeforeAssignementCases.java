package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Date;
import java.util.function.DoubleSupplier;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.CastMathOperandsBeforeAssignement;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestCastMathOperandsBeforeAssignementCases extends AJavaparserRefactorerCases {
	public static void main(String[] args) {
		System.out.println((long) (Integer.MAX_VALUE + 2));
		System.out.println((long) Integer.MAX_VALUE + 2);

		System.out.println("Multiple negative near overflow - asInt");
		int veryNegative = Integer.MIN_VALUE + 2;
		System.out.println(veryNegative * veryNegative);
		System.out.println((int) ((long) veryNegative * (long) veryNegative));

		System.out.println("Multiple negative near overflow - asLong");
		System.out.println(veryNegative * veryNegative);
		System.out.println((long) veryNegative * veryNegative);
	}

	@Override
	public IJavaparserMutator getTransformer() {
		return new CastMathOperandsBeforeAssignement();
	}

	@CompareMethods
	public static class FloatDivision {
		public float pre() {
			return 2 / 3;
		}

		public float post() {
			return 2F / 3;
		}
	}

	@CompareMethods
	public static class IntMultiplication {
		public long pre() {
			return 1_000 * 3_600 * 24 * 365;
		}

		public long post() {
			return 1_000L * 3_600 * 24 * 365;
		}
	}

	@CompareMethods
	public static class IntSum {
		public long pre() {
			return Integer.MAX_VALUE + 2;
		}

		public long post() {
			return Integer.MAX_VALUE + 2L;
		}
	}

	@CompareMethods
	public static class IntSum_toNumber {
		public Number pre() {
			return Integer.MAX_VALUE + 2;
		}

		public Number post() {
			return Integer.MAX_VALUE + 2L;
		}
	}

	@CompareMethods
	public static class IntSum_toFloat {
		public float pre() {
			return Integer.MAX_VALUE + 2;
		}

		public float post() {
			return Integer.MAX_VALUE + 2L;
		}
	}

	@CompareMethods
	public static class IntSum_toDouble {
		public double pre() {
			return Integer.MAX_VALUE + 2;
		}

		public double post() {
			return Integer.MAX_VALUE + 2L;
		}
	}

	@CompareMethods
	public static class IntSum_FieldRefs {
		public long pre() {
			return Integer.MAX_VALUE + Integer.MAX_VALUE;
		}

		public long post() {
			return (long) Integer.MAX_VALUE + Integer.MAX_VALUE;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class MethodCall_intToLong {
		public Date pre(int seconds) {
			return new Date(seconds * 1_000);
		}

		public Date post(int seconds) {
			return new Date(seconds * 1_000L);
		}
	}

	@UnmodifiedMethod
	public static class MethodCall_int {
		public String pre(int i) {
			return Integer.toString(1 + i);
		}
	}

	// Noncompliant, won't produce the expected result if factor > 214_748
	@CompareMethods
	public static class MultiplyIntsToDouble {
		public double pre(int factor) {
			return factor * 10_000;
		}

		public double post(int factor) {
			return factor * 10_000L;
		}
	}

	@UnmodifiedMethod
	public static class MultiplyIntsToInt {
		public int pre(int factor) {
			return factor * 10_000;
		}
	}

	// Noncompliant, will be rounded to closest long integer
	@CompareMethods
	public static class DivisionToFloat {
		public float pre(long factor) {
			return factor / 123;
		}

		public float post(long factor) {
			return factor / 123F;
		}
	}

	// Noncompliant, will be rounded to closest long integer
	// BEWARE Unclear if we should divide by `123F` or `123D`
	@CompareMethods
	public static class DivisionToDouble {
		public double pre(long factor) {
			return factor / 123;
		}

		public double post(long factor) {
			return factor / 123F;
		}
	}

	@UnmodifiedMethod
	public static class Division_longDividedByInt_ToLong {
		public long pre(long bytes) {
			return bytes / 1024;
		}
	}

	@UnmodifiedMethod
	public static class Division_longDividedByInt_ToLong_methodCall {
		public String pre(long bytes) {
			return Long.toString(bytes / 1024);
		}
	}

	@CompareMethods
	public static class Lambda {
		public DoubleSupplier pre() {
			return () -> 2 / 3;
		}

		public DoubleSupplier post() {
			return () -> 2F / 3;
		}
	}

	@CompareMethods
	public static class Deep_toLong {
		public long pre() {
			return (1 + (3 + 4)) + ((5 + (7 + 8)) + 3);
		}

		public long post() {
			return (1 + (3L + 4)) + ((5 + (7L + 8)) + 3);
		}
	}

	@UnmodifiedMethod
	public static class Deep_toInt {
		public int pre() {
			return (1 + (3 + 4)) + ((5 + (7 + 8)) + 3);
		}
	}

}
