package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.rules.EnumsWithoutEquals;
import eu.solven.cleanthat.rules.EnumsWithoutEqualsCases;

public class TestEnumsWithoutEquals extends ATestCases {

	@Ignore("Known to fail given it is hard to which if a type is an Enum given code only (and no loaded Class)")
	@Test
	public void testCases() throws IOException {
		testCasesIn(EnumsWithoutEqualsCases.class, new EnumsWithoutEquals());
	}
}
