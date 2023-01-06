package eu.solven.cleanthat.language.java.refactorer.java;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.StreamAnyMatchCases;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestStreamAnyMatchCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new StreamAnyMatchCases());
	}
}
