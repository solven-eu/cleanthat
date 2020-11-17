package io.cormoran.cleanthat.rules;

import java.io.IOException;

import org.junit.Test;

import eu.solven.cleanthat.rules.CreateTempFilesUsingNio;
import eu.solven.cleanthat.rules.CreateTempFilesUsingNioCases;

public class TestCreateTempFilesUsingNio extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(CreateTempFilesUsingNioCases.class, new CreateTempFilesUsingNio());
	}
}
