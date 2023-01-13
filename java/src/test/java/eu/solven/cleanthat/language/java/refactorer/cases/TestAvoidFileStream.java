package eu.solven.cleanthat.language.java.refactorer.cases;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.AvoidFileStreamCases;
import eu.solven.cleanthat.language.java.refactorer.mutators.AvoidFileStream;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestAvoidFileStream extends ATestCases {

	@Ignore
	@Test
	public void testCases() throws IOException {
		testCasesIn(AvoidFileStreamCases.class, new AvoidFileStream());
	}
}
