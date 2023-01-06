package eu.solven.cleanthat.language.java.refactorer.java;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.UseDiamondOperatorJdk8Cases;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestUseDiamondOperatorJdk8Cases extends ATestCases {

	@Ignore("TODO")
	@Test
	public void testCases() throws IOException {
		testCasesIn(new UseDiamondOperatorJdk8Cases());
	}
}
