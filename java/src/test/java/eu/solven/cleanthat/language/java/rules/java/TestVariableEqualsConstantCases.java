package eu.solven.cleanthat.language.java.rules.java;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.language.java.rules.cases.VariableEqualsConstantCases;
import eu.solven.cleanthat.language.java.rules.test.ATestCases;

public class TestVariableEqualsConstantCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new VariableEqualsConstantCases());
	}
}
