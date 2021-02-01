package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.SwitchVariableEqualsConstant;
import eu.solven.cleanthat.rules.cases.SwitchNumberToValueOfCases;
import eu.solven.cleanthat.rules.cases.SwitchVariableEqualsConstantCases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestSwitchVariableEqualsConstantCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new SwitchVariableEqualsConstantCases());
	}
}
