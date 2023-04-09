package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIndexOfChar;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestUseIndexOfCharCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new UseIndexOfChar();
	}

	@CompareMethods
	public static class indexOf {
		public Object pre(String s) {
			return s.indexOf("d");
		}

		public Object post(String s) {
			return s.indexOf('d');
		}
	}

	@CompareMethods
	public static class lastIndexOf {
		public Object pre(String s) {
			return s.lastIndexOf("d");
		}

		public Object post(String s) {
			return s.lastIndexOf('d');
		}
	}

	@UnmodifiedMethod
	public static class LongString {
		public Object pre(String s) {
			return s.indexOf("abc");
		}
	}

	@CompareMethods
	public static class indexOf_EmptyString {
		public Object pre(String s) {
			return s.indexOf("");
		}

		public Object post(String s) {
			return 0;
		}
	}

	@CompareMethods
	public static class lastIndexOf_EmptyString {
		public Object pre(String s) {
			return s.lastIndexOf("");
		}

		public Object post(String s) {
			return s.length();
		}
	}

}