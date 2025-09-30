package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ConsecutiveLiteralAppends;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestConsecutiveLiteralAppendsCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserAstMutator getTransformer() {
		return new ConsecutiveLiteralAppends();
	}

	@UnmodifiedMethod
	public static class FakeAppend {
		private void append(char c) { }

		public void pre() {
			append('/');
		}
	}

	@UnmodifiedMethod
	public static class Literal {
		public Object pre(StringBuilder builder) {
			return builder.append("first");
		}
	}

	@UnmodifiedMethod
	public static class Variable {
		public Object pre(StringBuilder builder, String first) {
			return builder.append(first);
		}
	}

	@CompareMethods
	public static class TwoLiterals {
		public Object pre(StringBuilder builder) {
			return builder.append("app").append("end");
		}

		public Object post(StringBuilder builder) {
			return builder.append("append");
		}
	}

	@CompareMethods
	public static class TwoChars {
		public Object pre(StringBuilder builder) {
			return builder.append('a').append('b');
		}

		public Object post(StringBuilder builder) {
			return builder.append("ab");
		}
	}

	@UnmodifiedMethod
	public static class TwoVariables {
		public Object pre(StringBuilder builder, String first, String second) {
			return builder.append(first).append(second);
		}
	}

	@CompareMethods
	public static class CharAndString {
		public Object pre(StringBuilder builder) {
			return builder.append('a').append("ppend");
		}

		public Object post(StringBuilder builder) {
			return builder.append("append");
		}
	}

	@CompareMethods
	public static class StringAndChar {
		public Object pre(StringBuilder builder) {
			return builder.append("map").append('s');
		}

		public Object post(StringBuilder builder) {
			return builder.append("maps");
		}
	}

	@UnmodifiedMethod
	public static class VariableAndLiteral {
		public Object pre(StringBuilder builder, String first) {
			return builder.append(first).append("second");
		}
	}

	@UnmodifiedMethod
	public static class LiteralAndVariable {
		public Object pre(StringBuilder builder, String second) {
			return builder.append("first").append(second);
		}
	}

	@CompareMethods
	public static class TwoIntegers {
		public Object pre(StringBuilder builder) {
			return builder.append(1).append(2);
		}

		public Object post(StringBuilder builder) {
			return builder.append("12");
		}
	}

	@UnmodifiedMethod
	public static class NonSingleDigitIntegers {
		public Object pre(StringBuilder builder) {
			return builder.append(123).append(456);
		}
	}

	@UnmodifiedMethod
	public static class IntegerOverflow {
		public Object pre(StringBuilder builder) {
			return builder.append(2147483647).append(1);
		}
	}

	@UnmodifiedMethod
	public static class NegativeInteger {
		public Object pre(StringBuilder builder) {
			return builder.append(1).append(-2);
		}
	}

	@CompareMethods
	public static class HexadecimalIntegers {
		public Object pre(StringBuilder builder) {
			return builder.append(0x1).append(0x2);
		}

		public Object post(StringBuilder builder) {
			return builder.append("12");
		}
	}

	@UnmodifiedMethod
	public static class CastIntegers {
		public Object pre(StringBuilder builder) {
			return builder.append((char) 1).append((char) 2);
		}
	}

}
