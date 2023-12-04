package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StringFromString;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestStringFromStringCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new StringFromString();
	}

	@CompareMethods
	public static class CaseString {
		public Object pre() {
			return new String("StringLiteral");
		}

		public Object post() {
			return "StringLiteral";
		}
	}

	@CompareMethods
	public static class CaseEnclosed {
		public Object pre() {
			return new String(("StringLiteral"));
		}

		public Object post() {
			return "StringLiteral";
		}
	}

	@CompareMethods
	public static class CaseString_argument {
		public Object pre(String o) {
			return new String(o);
		}

		public Object post(String o) {
			return o;
		}
	}

	@CompareMethods
	public static class CaseCharSequence {
		public Object pre(CharSequence o) {
			return new String(o.toString());
		}

		public Object post(CharSequence o) {
			return o.toString();
		}
	}

	@UnmodifiedMethod
	public static class CaseStringBuilder {
		public Object pre(StringBuilder o) {
			return new String(o);
		}
	}

}
