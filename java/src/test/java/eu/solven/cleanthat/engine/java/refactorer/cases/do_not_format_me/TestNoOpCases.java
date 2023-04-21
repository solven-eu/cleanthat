package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.NoOpJavaParserRule;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestNoOpCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new NoOpJavaParserRule();
	}

	@UnmodifiedMethod
	public static class EmptyRowBetweenComments {
		public void pre() {
			// Comment before empty row

			// Comment after empty row
		}
	}

	/**
	 * 
	 * JavaParser tends to remove empty empty lines from Javadoc
	 * 
	 * @author Benoit Lacelle
	 *
	 */
	@UnmodifiedMethod
	public static class EmptyRowBetweenComments_withAdditions {
		public String pre() {
			String string = "a";
			string += "b";

			string += "c";
			string += "d";

			return string;
		}
	}

}