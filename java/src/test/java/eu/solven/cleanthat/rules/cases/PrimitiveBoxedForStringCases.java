package eu.solven.cleanthat.rules.cases;

import eu.solven.cleanthat.language.java.rules.PrimitiveBoxedForString;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.cases.annotations.CompareMethods;
import eu.solven.cleanthat.rules.test.ACases;

public class PrimitiveBoxedForStringCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new PrimitiveBoxedForString();
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
}
