package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.rules.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.rules.cases.UseDiamondOperatorJdk8Cases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestUseDiamondOperatorJdk8Cases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(UseDiamondOperatorJdk8Cases.class, new UseDiamondOperatorJdk8());
	}
}
