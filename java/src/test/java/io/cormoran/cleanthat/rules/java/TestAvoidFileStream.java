package io.cormoran.cleanthat.rules.java;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.language.java.rules.AvoidFileStream;
import eu.solven.cleanthat.rules.cases.AvoidFileStreamCases;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestAvoidFileStream extends ATestCases {

	@Ignore
	@Test
	public void testCases() throws IOException {
		testCasesIn(AvoidFileStreamCases.class, new AvoidFileStream());
	}
}
