package eu.solven.cleanthat.language.java.refactorer.java;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.ReorderModifiersCases;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestReorderModifiersCases extends ATestCases {

	@Ignore("TODO")
	@Test
	public void testCases() throws IOException {
		testCasesIn(new ReorderModifiersCases());
	}
}
