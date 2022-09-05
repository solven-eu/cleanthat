package eu.solven.cleanthat.language.java.rules.java;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.language.java.rules.cases.do_not_format_me.SwitchNumberToValueOfCases;
import eu.solven.cleanthat.language.java.rules.test.ATestCases;

public class TestSwitchNumberToValueOfCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new SwitchNumberToValueOfCases());
	}
}
