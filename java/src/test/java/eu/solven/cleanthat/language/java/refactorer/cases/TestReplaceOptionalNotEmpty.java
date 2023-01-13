package eu.solven.cleanthat.language.java.refactorer.cases;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.OptionalNotEmptyCases;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestReplaceOptionalNotEmpty extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new OptionalNotEmptyCases());
	}
}
