package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.PrimitiveBoxedForString;
import eu.solven.cleanthat.rules.PrimitiveBoxedForStringCases;

public class TestPrimitiveBoxedForString extends ATestCases {

	@Test
	public void testCases_JavaParsdr() throws IOException {
		testCasesIn(PrimitiveBoxedForStringCases.class, new PrimitiveBoxedForString());
	}
}
