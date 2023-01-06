package eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.mutators.NumberToValueOf;
import eu.solven.cleanthat.language.java.refactorer.test.ACases;

public class SwitchNumberToValueOfCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new NumberToValueOf();
	}

}
