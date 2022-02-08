package eu.solven.cleanthat.language.java.rules.cases;

import eu.solven.cleanthat.language.java.rules.NoOpJavaParserRule;
import eu.solven.cleanthat.language.java.rules.cases.annotations.UnchangedMethod;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.test.ACases;

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