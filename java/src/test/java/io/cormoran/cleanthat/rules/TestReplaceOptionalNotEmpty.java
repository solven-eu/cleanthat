package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.rules.ReplaceOptionalNotEmpty;
import eu.solven.cleanthat.rules.ReplaceOptionalNotEmptyCases;

public class TestReplaceOptionalNotEmpty extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(ReplaceOptionalNotEmptyCases.class, new ReplaceOptionalNotEmpty());
	}
}
