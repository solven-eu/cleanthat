package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.EnumsWithoutEquals;
import eu.solven.cleanthat.rules.EnumsWithoutEqualsCases;

public class TestEnumsWithoutEquals extends ATestCases {
	@Test
	public void testCases_JavaParsdr() throws IOException {
		testCasesIn(EnumsWithoutEqualsCases.class, new EnumsWithoutEquals());
	}
}
