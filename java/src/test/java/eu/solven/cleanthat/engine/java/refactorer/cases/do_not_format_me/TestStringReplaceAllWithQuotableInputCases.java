package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StringReplaceAllWithQuotableInput;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestStringReplaceAllWithQuotableInputCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new StringReplaceAllWithQuotableInput();
	}

	@UnmodifiedMethod
	public static class ComplexRegex {
		public Object pre(String s) {
			return s.replaceAll("\\w*\\sis", "It's");
		}
	}

	@UnmodifiedMethod
	public static class InvalidRegex {
		public Object pre(String s) {
			return s.replaceAll("*", "It's");
		}
	}

	@CompareMethods
	public static class SimpleText {
		public Object pre(String s) {
			return s.replaceAll("Bob is", "It's");
		}

		public Object post(String s) {
			return s.replace("Bob is", "It's");
		}
	}

	@CompareMethods
	public static class SimpleTextAndEscapedCharacters {
		public Object pre(String s) {
			return s.replaceAll("\\.\\.\\.", ";");
		}

		public Object post(String s) {
			return s.replace("...", ";");
		}
	}

	@CompareMethods
	public static class SimpleTextAndEscapedCharacters_Variety {
		public Object pre(String s) {
			return s.replaceAll("\\.\\\\\\{", ";");
		}

		public Object post(String s) {
			return s.replace(".\\{", ";");
		}
	}

	@UnmodifiedMethod
	public static class ComplexRegex_lookingLikeEscape {
		public Object pre(String s) {
			return s.replaceAll("\\.{3}", ";");
		}
	}

	// `\R` matches any Unicode linebreak sequence
	@UnmodifiedMethod
	public static class LineBreaks {
		public Object pre(String s) {
			return s.replaceAll("\\R", "");
		}
	}

}
