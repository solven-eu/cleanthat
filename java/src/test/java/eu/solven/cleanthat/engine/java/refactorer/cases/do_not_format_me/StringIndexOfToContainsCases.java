package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StringIndexOfToContains;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class StringIndexOfToContainsCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new StringIndexOfToContains();
	}

	@CompareMethods
	public static class Nominal {
		public boolean pre(String string, String subString) {
			return string.indexOf(subString) >= 0;
		}

		public boolean post(String string, String subString) {
			return string.contains(subString);
		}
	}

	@UnmodifiedMethod
	public static class CpmpareWith1 {
		public boolean pre(String string, String subString) {
			return string.indexOf(subString) >= 1;
		}
	}

	@UnmodifiedMethod
	public static class IndexOfEquals0 {
		public boolean pre(String string, String subString) {
			return string.indexOf(subString) == 0;
		}
	}

	@UnmodifiedMethod
	public static class IndexOfLowerEquals0 {
		public boolean pre(String string, String subString) {
			return string.indexOf(subString) <= 0;
		}
	}

	@CompareMethods
	public static class IndexOfNegative {
		public boolean pre(String string, String subString) {
			return string.indexOf(subString) < 0;
		}

		public boolean post(String string, String subString) {
			return !string.contains(subString);
		}
	}

	@CompareMethods
	public static class ZeroLowerThanIndexOf {
		public boolean pre(String string, String subString) {
			return 0 <= string.indexOf(subString);
		}

		public boolean post(String string, String subString) {
			return string.contains(subString);
		}
	}

}