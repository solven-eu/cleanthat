package eu.solven.cleanthat.language.java.refactorer.cases;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.EnumsWithoutEqualsCases;
import eu.solven.cleanthat.language.java.refactorer.mutators.EnumsWithoutEquals;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestEnumsWithoutEquals extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(EnumsWithoutEqualsCases.class, new EnumsWithoutEquals());
	}
}
