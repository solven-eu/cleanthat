package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.PreferConstantsAsEqualsLeftOperator;
import eu.solven.cleanthat.rules.PreferConstantsAsEqualsLeftOperatorCases;

public class TestPreferConstantsAsEqualsLeftOperator extends ATestCases {

	@Ignore("TODO")
	@Test
	public void testCases() throws IOException {
		testCasesIn(PreferConstantsAsEqualsLeftOperatorCases.class, new PreferConstantsAsEqualsLeftOperator());
	}
}