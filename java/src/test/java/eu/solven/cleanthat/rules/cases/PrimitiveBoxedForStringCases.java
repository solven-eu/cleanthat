package eu.solven.cleanthat.rules.cases;

import eu.solven.cleanthat.rules.PrimitiveBoxedForString;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;
import eu.solven.cleanthat.rules.test.ICaseOverMethod;

public class PrimitiveBoxedForStringCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new PrimitiveBoxedForString();
	}

	public static class CaseBoolean_valueOf implements ICaseOverMethod {
		public Object pre(boolean input) {
			return Boolean.valueOf(input).toString();
		}

		public Object post(boolean input) {
			return Boolean.toString(input);
		}
	}

	public static class CaseBoolean_consctructor implements ICaseOverMethod {
		public Object pre(boolean input) {
			return new Boolean(input).toString();
		}

		public Object post(boolean input) {
			return Boolean.toString(input);
		}
	}

	public static class CaseByte implements ICaseOverMethod {
		public Object pre(byte input) {
			return Byte.valueOf(input).toString();
		}

		public Object post(byte input) {
			return Byte.toString(input);
		}
	}

	public static class CaseShort implements ICaseOverMethod {
		public Object pre(short input) {
			return Short.valueOf(input).toString();
		}

		public Object post(short input) {
			return Short.toString(input);
		}
	}

	public static class CaseInteger implements ICaseOverMethod {
		public Object pre(int input) {
			return Integer.valueOf(input).toString();
		}

		public Object post(int input) {
			return Integer.toString(input);
		}
	}

	public static class CaseLong implements ICaseOverMethod {
		public Object pre(long input) {
			return Long.valueOf(input).toString();
		}

		public Object post(long input) {
			return Long.toString(input);
		}
	}

	public static class CaseFloat implements ICaseOverMethod {
		public Object pre(float someByte) {
			return Float.valueOf(someByte).toString();
		}

		public Object post(float someByte) {
			return Float.toString(someByte);
		}
	}

	public static class CaseDouble implements ICaseOverMethod {
		public Object pre(double someByte) {
			return Double.valueOf(someByte).toString();
		}

		public Object post(double someByte) {
			return Double.toString(someByte);
		}
	}
}
