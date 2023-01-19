package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIndexOfChar;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class UseIndexOfCharCases extends ARefactorerCases {
	@Override
	public IClassTransformer getTransformer() {
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