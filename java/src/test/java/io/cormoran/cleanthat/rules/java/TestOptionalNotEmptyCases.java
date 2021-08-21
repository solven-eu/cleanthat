package io.cormoran.cleanthat.rules.java;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.rules.cases.OptionalNotEmptyCases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestOptionalNotEmptyCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new OptionalNotEmptyCases());
	}
}
