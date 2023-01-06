package eu.solven.cleanthat.language.java.refactorer.java;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.SwitchNumberToValueOfCases;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestSwitchNumberToValueOfCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new SwitchNumberToValueOfCases());
	}
}
