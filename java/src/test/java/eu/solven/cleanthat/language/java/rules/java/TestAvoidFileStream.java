package eu.solven.cleanthat.language.java.rules.java;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.language.java.rules.cases.do_not_format_me.AvoidFileStreamCases;
import eu.solven.cleanthat.language.java.rules.mutators.AvoidFileStream;
import eu.solven.cleanthat.language.java.rules.test.ATestCases;

public class TestAvoidFileStream extends ATestCases {

	@Ignore
	@Test
	public void testCases() throws IOException {
		testCasesIn(AvoidFileStreamCases.class, new AvoidFileStream());
	}
}
