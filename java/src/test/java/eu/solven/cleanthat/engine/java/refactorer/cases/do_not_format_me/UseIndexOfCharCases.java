package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIndexOfChar;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class UseIndexOfCharCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new UseIndexOfChar();
	}

	@CompareMethods
	public static class JavaLangType {
		public Object pre(String s) {
			return s.indexOf("d");
		}

		public Object post(String s) {
			return s.indexOf('d');
		}
	}

}