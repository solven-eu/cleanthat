package eu.solven.cleanthat.language.java.refactorer.java;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.JUnit4ToJUnit5Cases;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestJUnit4ToJUnit5Cases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new JUnit4ToJUnit5Cases());
	}
}