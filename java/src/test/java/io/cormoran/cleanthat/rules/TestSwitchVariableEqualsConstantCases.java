package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.cases.VariableEqualsConstantCases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestSwitchVariableEqualsConstantCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new VariableEqualsConstantCases());
	}
}
