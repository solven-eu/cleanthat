package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StringStartsWithChar;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class StringStartsWithCharCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new StringStartsWithChar();
	}

	@CompareMethods
	public static class startsWithElseIsEmpty {
		public Object pre(String s) {
			return s.startsWith("#") || s.isEmpty();
		}

		public Object post(String s) {
			return s.isEmpty() || s.charAt(0) == '#';
		}
	}

	@CompareMethods
	public static class isEmptyOrStartsWith {
		public Object pre(String s) {
			return s.startsWith("#") || s.isEmpty();
		}

		public Object post(String s) {
			return s.isEmpty() || s.charAt(0) == '#';
		}
	}

	@UnmodifiedMethod
	public static class differentScope {
		public Object pre(String s1, String s2) {
			return s1.startsWith("#") || s2.isEmpty();
		}
	}

}