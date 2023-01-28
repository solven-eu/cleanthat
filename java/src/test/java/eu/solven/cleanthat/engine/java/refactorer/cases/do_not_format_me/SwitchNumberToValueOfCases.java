package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.engine.java.refactorer.mutators.NumberToValueOf;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class SwitchNumberToValueOfCases extends ARefactorerCases {
	@Override
	public IClassTransformer getTransformer() {
		return new NumberToValueOf();
	}

}
