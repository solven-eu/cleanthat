package eu.solven.cleanthat.language.java.rules.cases.junit4tojunit5;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// https://dimitrisli.wordpress.com/2011/03/06/junit-showcase-setting-up-and-optimising-unit-tests/
public class BeforeAfterTestPost {

	@BeforeAll
	public static void initialiseClass() {
		System.out.println("init class");
	}

	@BeforeEach
	public void initialiseTest() {
		System.out.println("init test");
	}

	@Test
	public void test1() {
		System.out.println("inside test1");
		Assertions.assertTrue(true);
	}

	@Test
	public void test2() {
		System.out.println("inside test2");
		Assertions.assertTrue(true);
	}

	@AfterEach
	public void teardownTest() {
		System.out.println("teardown test");
	}

	@AfterAll
	public static void teardownClass() {
		System.out.println("teardown class");
	}
}
