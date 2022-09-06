package eu.solven.cleanthat.language.java.rules.java;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.language.java.rules.cases.do_not_format_me.NoOpCases;
import eu.solven.cleanthat.language.java.rules.test.ATestCases;

public class TestNoOpCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new NoOpCases());
	}
}
