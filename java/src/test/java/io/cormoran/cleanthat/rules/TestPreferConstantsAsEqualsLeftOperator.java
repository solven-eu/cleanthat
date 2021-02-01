package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.rules.PreferConstantsAsEqualsLeftOperator;
import eu.solven.cleanthat.rules.cases.PreferConstantsAsEqualsLeftOperatorCases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestPreferConstantsAsEqualsLeftOperator extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(PreferConstantsAsEqualsLeftOperatorCases.class, new PreferConstantsAsEqualsLeftOperator());
	}
}
