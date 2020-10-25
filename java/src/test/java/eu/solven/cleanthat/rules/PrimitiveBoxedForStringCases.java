package eu.solven.cleanthat.rules;

import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class PrimitiveBoxedForStringCases {
	public String getId() {
		return "PrimitiveBoxedForString";
	}

	public IClassTransformer getTransformer() {
		return new PrimitiveBoxedForString();
	}

	public static class CaseBoolean {
		public String getTitle() {
			return "Boolean";
		}

		public Object pre(boolean input) {
			return new Boolean(input).toString();
		}

		public Object post(boolean input) {
			return Boolean.toString(input);
		}
	}

	public static class CaseByte {
		public String getTitle() {
			return "Byte";
		}

		public Object pre(byte input) {
			return new Byte(input).toString();
		}

		public Object post(byte input) {
			return Byte.toString(input);
		}
	}

	public static class CaseShort {
		public String getTitle() {
			return "Short";
		}

		public Object pre(short input) {
			return new Short(input).toString();
		}

		public Object post(short input) {
			return Short.toString(input);
		}
	}

	public static class CaseInteger {
		public String getTitle() {
			return "Integer";
		}

		public Object pre(int input) {
			return new Integer(input).toString();
		}

		public Object post(int input) {
			return Integer.toString(input);
		}
	}

	public static class CaseLong {
		public String getTitle() {
			return "Long";
		}

		public Object pre(long input) {
			return new Long(input).toString();
		}

		public Object post(long input) {
			return Long.toString(input);
		}
	}

	public static class CaseFloat {
		public String getTitle() {
			return "Float";
		}

		public Object pre(float someByte) {
			return new Float(someByte).toString();
		}

		public Object post(float someByte) {
			return Float.toString(someByte);
		}
	}

	public static class CaseDouble {
		public String getTitle() {
			return "Double";
		}

		public Object pre(double someByte) {
			return new Double(someByte).toString();
		}

		public Object post(double someByte) {
			return Double.toString(someByte);
		}
	}
}
