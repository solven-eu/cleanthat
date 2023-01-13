package eu.solven.cleanthat.language.java.refactorer.cases;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.UseIndexOfCharCases;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestUseIndexOfChar extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new UseIndexOfCharCases());
	}
}
