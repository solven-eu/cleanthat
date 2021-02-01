package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.cases.ReorderModifiersCases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestReorderModifiersCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new ReorderModifiersCases());
	}
}
