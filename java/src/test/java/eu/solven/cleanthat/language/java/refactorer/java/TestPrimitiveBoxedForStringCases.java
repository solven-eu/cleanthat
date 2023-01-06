package eu.solven.cleanthat.language.java.refactorer.java;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.PrimitiveBoxedForStringCases;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestPrimitiveBoxedForStringCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new PrimitiveBoxedForStringCases());
	}
}
