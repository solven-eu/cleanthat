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
		public Object pre() {
			return new StringBuilder().append("first");
		}
	}

	@UnmodifiedMethod
	public static class Variable {
		public Object pre(String first) {
			return new StringBuilder().append(first);
		}
	}

	@CompareMethods
	public static class TwoLiterals {
		public Object pre() {
			return new StringBuilder().append("app").append("end");
		}

		public Object post() {
			return new StringBuilder().append("append");
		}
	}

	@CompareMethods
	public static class TwoChars {
		public Object pre() {
			return new StringBuilder().append('a').append('b');
		}

		public Object post() {
			return new StringBuilder().append("ab");
		}
	}

	@UnmodifiedMethod
	public static class TwoVariables {
		public Object pre(String first, String second) {
			return new StringBuilder().append(first).append(second);
		}
	}

	@CompareMethods
	public static class CharAndString {
		public Object pre() {
			return new StringBuilder().append('a').append("ppend");
		}

		public Object post() {
			return new StringBuilder().append("append");
		}
	}

	@CompareMethods
	public static class StringAndChar {
		public Object pre() {
			return new StringBuilder().append("map").append('s');
		}

		public Object post() {
			return new StringBuilder().append("maps");
		}
	}

	@UnmodifiedMethod
	public static class VariableAndLiteral {
		public Object pre(String first) {
			return new StringBuilder().append(first).append("second");
		}
	}

	@UnmodifiedMethod
	public static class LiteralAndVariable {
		public Object pre(String second) {
			return new StringBuilder().append("first").append(second);
		}
	}

}
