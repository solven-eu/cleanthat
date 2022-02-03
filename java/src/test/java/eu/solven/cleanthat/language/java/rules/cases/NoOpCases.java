package eu.solven.cleanthat.language.java.rules.cases;

import eu.solven.cleanthat.language.java.rules.NoOpJavaParserRule;
import eu.solven.cleanthat.language.java.rules.cases.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.test.ACases;

public class NoOpCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new NoOpJavaParserRule();
	}

	// We do not rely @UnchangedMethod as this rule will return true on AST change

	@CompareMethods
	public static class EmptyRowBetweenComments {
		public void pre() {
			// Comment before empty row

			// Comment after empty row
		}

		public void post() {
			// Comment before empty row

			// Comment after empty row
		}
	}
}