package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.UseIsEmptyOnCollections;
import eu.solven.cleanthat.rules.UseIsEmptyOnCollectionsCases;

public class TestUseIsEmptyOnCollectionsCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(UseIsEmptyOnCollectionsCases.class, new UseIsEmptyOnCollections());
	}
}
