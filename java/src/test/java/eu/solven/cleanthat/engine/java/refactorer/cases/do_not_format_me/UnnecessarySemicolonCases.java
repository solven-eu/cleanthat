package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessarySemicolon;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class UnnecessarySemicolonCases extends AJavaparserRefactorerCases {
	static final String PREFIX = "source/do_not_format_me/UseTextBlocks/";

	@Override
	public IJavaparserMutator getTransformer() {
		return new UnnecessarySemicolon();
	}

	@CompareMethods
	public static class caseDoubleSemiColon {
		public int pre() {
			int i = 2;
			;

			return i;
		}

		public int post() {
			int i = 2;

			return i;
		}
	}

	@CompareMethods
	public static class caseAfterIf_singleStatement {
		public boolean pre(List<String> l) {
			if (l.remove(""))
				;

			return l.add("");
		}

		public boolean post(List<String> l) {
			l.remove("");

			return l.add("");
		}
	}

	@UnmodifiedMethod
	public static class caseAfterIf_multipleStatement {
		public boolean pre(List<String> l) {
			if (l.size() >= 1 && l.remove(""))
				;

			return l.add("");
		}
	}

	// https://rules.sonarsource.com/java/RSPEC-2959
	// TODO
	// @CompareMethods
	@UnmodifiedMethod
	public static class caseTry {
		public void pre() throws IOException {
			try (ByteArrayInputStream b = new ByteArrayInputStream(new byte[10]);
					Reader r = new InputStreamReader(b);) {
				// do stuff
			}
		}

		public void post() throws IOException {
			try (ByteArrayInputStream b = new ByteArrayInputStream(new byte[10]); Reader r = new InputStreamReader(b)) {
				// do stuff
			}
		}
	}

}
