package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.ReplaceOptionalNotEmpty;
import eu.solven.cleanthat.rules.ReplaceOptionalNotEmptyCases;

public class TestReplaceOptionalNotEmpty extends ATestCases {

	@Ignore("TODO")
	@Test
	public void testCases() throws IOException {
		testCasesIn(ReplaceOptionalNotEmptyCases.class, new ReplaceOptionalNotEmpty());
	}
}
