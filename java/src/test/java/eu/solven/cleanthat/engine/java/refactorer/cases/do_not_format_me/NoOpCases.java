package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.NoOpJavaParserRule;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class NoOpCases extends ARefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new NoOpJavaParserRule();
	}

	@UnchangedMethod
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
	@UnchangedMethod
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