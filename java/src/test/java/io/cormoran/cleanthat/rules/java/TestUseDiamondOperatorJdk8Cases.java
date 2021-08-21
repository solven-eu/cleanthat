package io.cormoran.cleanthat.rules.java;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.rules.cases.UseDiamondOperatorJdk8Cases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestUseDiamondOperatorJdk8Cases extends ATestCases {

	@Ignore("TODO")
	@Test
	public void testCases() throws IOException {
		testCasesIn(new UseDiamondOperatorJdk8Cases());
	}
}
