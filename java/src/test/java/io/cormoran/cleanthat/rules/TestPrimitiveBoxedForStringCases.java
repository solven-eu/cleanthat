package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.cases.PrimitiveBoxedForStringCases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestPrimitiveBoxedForStringCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new PrimitiveBoxedForStringCases());
	}
}
