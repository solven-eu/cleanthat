package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.time.LocalDate;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidInlineConditionals;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class AvoidInlineConditionalsCases extends ARefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new AvoidInlineConditionals();
	}

	@UnchangedMethod
	public static class Checkstyle_booleanExpr {
		public Object pre(int x) {
			boolean foobar = (x == 5);
			return foobar;
		}
	}

	// There is parenthesis around 'text == null'
	@CompareMethods
	public static class Checkstyle_simple_return {
		public Object pre(String text) {
			return (text == null) ? "" : text;
		}

		public Object post(String text) {
			if (text == null) {
				return "";
			} else {
				return text;
			}
		}
	}

	@CompareMethods
	public static class Checkstyle_simple_return_conditionNoBlock {
		public Object pre(String text) {
			return text == null ? "" : text;
		}

		public Object post(String text) {
			if (text == null) {
				return "";
			} else {
				return text;
			}
		}
	}

	@CompareMethods
	public static class Checkstyle_booleanExpression_variable {
		public Object pre(String text) {
			String output = (text == null) ? "" : text;
			return output;
		}

		public Object post(String text) {
			String output;
			if (text == null) {
				output = "";
			} else {
				output = text;
			}
			return output;
		}
	}

	@CompareMethods
	public static class Checkstyle_simple_return_withPrefix {
		public Object pre(String text) {
			LocalDate.now();
			return text == null ? "" : text;
		}

		public Object post(String text) {
			LocalDate.now();
			if (text == null) {
				return "";
			} else {
				return text;
			}
		}
	}

	@UnchangedMethod
	public static class Checkstyle_ok {
		public Object pre(String a) {
			String b;
			if (a != null && a.length() >= 1) {
				b = a.substring(1);
			} else {
				b = null;
			}
			return b;
		}
	}

	@CompareMethods
	public static class Checkstyle_complexExpression_return {
		public Object pre(String a) {
			return (a != null && a.length() >= 1) ? a.substring(1) : null;
		}

		public Object post(String a) {
			if (a != null && a.length() >= 1) {
				return a.substring(1);
			} else {
				return null;
			}
		}
	}

	@CompareMethods
	public static class Checkstyle_complexExpression_variable {
		public Object pre(String a) {
			Object output = (a != null && a.length() >= 1) ? a.substring(1) : null;
			return output;
		}

		public Object post(String a) {
			Object output;
			if (a != null && a.length() >= 1) {
				output = a.substring(1);
			} else {
				output = null;
			}
			return output;
		}
	}

	@UnchangedMethod
	public static class MultipleVariables {
		public Object pre() {
			int a = 2, b = a > 3 ? 5 : 7;
			return b;
		}
	}

	// BEWARE We should unroll once more
	// This would be done automatically by Spotless (which apply cleaning until idempotency)
	@CompareMethods
	public static class ImbricatedTernaries {
		public Object pre(int a) {
			return a > 2 ? (a < 3 ? 5 : 7) : 11;
		}

		public Object post(int a) {
			if (a > 2) {
				return (a < 3 ? 5 : 7);
			} else {
				return 11;
			}
		}
	}
}
