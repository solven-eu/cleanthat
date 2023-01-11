package eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.language.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.mutators.UseIndexOfChar;
import eu.solven.cleanthat.language.java.refactorer.test.ACases;

public class UseIndexOfCharCases extends ACases {
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