package io.cormoran.cleanthat.rules.java;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.language.java.rules.EnumsWithoutEquals;
import eu.solven.cleanthat.rules.cases.EnumsWithoutEqualsCases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestEnumsWithoutEquals extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(EnumsWithoutEqualsCases.class, new EnumsWithoutEquals());
	}
}
