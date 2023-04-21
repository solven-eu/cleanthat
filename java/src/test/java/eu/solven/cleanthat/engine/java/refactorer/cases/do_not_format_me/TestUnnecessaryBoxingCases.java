package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Set;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryBoxing;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestUnnecessaryBoxingCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new UnnecessaryBoxing();
	}

	@CompareMethods
	public static class CaseBoolean_valueOf {
		public Object pre(boolean input) {
			return Boolean.valueOf(input).toString();
		}

		public Object post(boolean input) {
			return Boolean.toString(input);
		}
	}

	@CompareMethods
	public static class CaseBoolean_consctructor {
		public Object pre(boolean input) {
			return new Boolean(input).toString();
		}

		public Object post(boolean input) {
			return Boolean.toString(input);
		}
	}

	@CompareMethods
	public static class CaseByte {
		public Object pre(byte input) {
			return Byte.valueOf(input).toString();
		}

		public Object post(byte input) {
			return Byte.toString(input);
		}
	}

	@CompareMethods
	public static class CaseShort {
		public Object pre(short input) {
			return Short.valueOf(input).toString();
		}

		public Object post(short input) {
			return Short.toString(input);
		}
	}

	@CompareMethods
	public static class CaseInteger {
		public Object pre(int input) {
			return Integer.valueOf(input).toString();
		}

		public Object post(int input) {
			return Integer.toString(input);
		}
	}

	@CompareMethods
	public static class CaseLong {
		public Object pre(long input) {
			return Long.valueOf(input).toString();
		}

		public Object post(long input) {
			return Long.toString(input);
		}
	}

	@CompareMethods
	public static class CaseFloat {
		public Object pre(float someByte) {
			return Float.valueOf(someByte).toString();
		}

		public Object post(float someByte) {
			return Float.toString(someByte);
		}
	}

	@CompareMethods
	public static class CaseDouble {
		public Object pre(double someByte) {
			return Double.valueOf(someByte).toString();
		}

		public Object post(double someByte) {
			return Double.toString(someByte);
		}
	}

	// TODO
	@UnmodifiedMethod
	// @CompareMethods
	public static class CaseIntegerToPrimitive {
		public Object pre(int i) {
			int i1 = Integer.valueOf(i).intValue();
			int i2 = Integer.valueOf(i);

			return i1 + i2;
		}

		public Object post(int i) {
			int i1 = i;
			int i2 = i;

			return i1 + i2;
		}
	}

	// TODO
	@UnmodifiedMethod
	// @CompareMethods
	public static class CaseLongToInt {
		public boolean pre(Set<Integer> set, long l) {
			return set.contains(Long.valueOf(l).intValue());
		}

		public boolean post(Set<Integer> set, long l) {
			return set.contains((int) l);
		}
	}
}
