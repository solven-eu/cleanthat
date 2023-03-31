package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyStartsWith;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

@Deprecated(since = "Dropped with PMD 7.0")
public class SimplifyStartsWithCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new SimplifyStartsWith();
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
			return s.isEmpty() || s.startsWith("#");
		}

		public Object post(String s) {
			return s.isEmpty() || s.charAt(0) == '#';
		}
	}

	@CompareMethods
	public static class startsWithAndNotIsEmpty {
		public Object pre(String s) {
			return s.startsWith("#") && !s.isEmpty();
		}

		public Object post(String s) {
			return !s.isEmpty() && s.charAt(0) == '#';
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class startsWithOrIsEmptyAndSomething {
		public Object pre(String s, boolean andIsEmpty) {
			return s.startsWith("#") || s.isEmpty() && andIsEmpty;
		}

		public Object post(String s, boolean andIsEmpty) {
			return s.isEmpty() && andIsEmpty || !s.isEmpty() && s.charAt(0) == '#';
		}
	}

	@UnmodifiedMethod
	public static class differentScope {
		public Object pre(String s1, String s2) {
			return s1.startsWith("#") || s2.isEmpty();
		}
	}

	// May be relevant for performance, but not readability
	@UnmodifiedMethod
	public static class startsWithSingleCharString {
		public Object pre(String s) {
			return s.startsWith("#");
		}

		public Object post(String s) {
			return !s.isEmpty() && s.charAt(0) == '#';
		}
	}

	// May be relevant for performance, but not readability
	@UnmodifiedMethod
	public static class startsWith_withOffset {
		public Object pre(String s) {
			return s.startsWith("#", 3);
		}

		public Object post(String s) {
			return s.length() >= 4 && s.charAt(4) == '#';
		}
	}

	@CompareMethods
	public static class startsWith_multipleOptions {
		public Object pre(String line) {
			return line.isEmpty() || line.startsWith("#") || line.startsWith(";");
		}

		public Object post(String line) {
			return line.isEmpty() || line.charAt(0) == '#' || line.startsWith(";");
		}

		// TODO
		public Object post_better(String line) {
			return line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == ';';
		}
	}

}