package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.NumberToValueOf;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class SwitchNumberToValueOfCases extends ARefactorerCases {
	@Override
	public IMutator getTransformer() {
		return new NumberToValueOf();
	}

}
