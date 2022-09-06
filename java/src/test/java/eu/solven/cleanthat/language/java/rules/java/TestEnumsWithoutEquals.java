package eu.solven.cleanthat.language.java.rules.java;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.language.java.rules.cases.do_not_format_me.EnumsWithoutEqualsCases;
import eu.solven.cleanthat.language.java.rules.mutators.EnumsWithoutEquals;
import eu.solven.cleanthat.language.java.rules.test.ATestCases;

public class TestEnumsWithoutEquals extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(EnumsWithoutEqualsCases.class, new EnumsWithoutEquals());
	}
}
