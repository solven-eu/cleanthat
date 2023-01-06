package eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.language.java.refactorer.NoOpJavaParserRule;
import eu.solven.cleanthat.language.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.test.ACases;

public class NoOpCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new NoOpJavaParserRule();
	}

	@UnchangedMethod
	public static class EmptyRowBetweenComments {
		public void post() {
			// Comment before empty row

			// Comment after empty row
		}
	}

	@UnchangedMethod
	public static class EmptyRowBetweenComments_withAditions {
		public String post() {
			String string = "a";
			string += "b";

			string += "c";
			string += "d";

			return string;
		}
	}

}