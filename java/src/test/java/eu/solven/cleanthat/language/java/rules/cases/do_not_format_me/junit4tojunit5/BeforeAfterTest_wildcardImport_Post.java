package eu.solven.cleanthat.language.java.rules.cases.do_not_format_me.junit4tojunit5;

import org.junit.jupiter.api.*;

// https://dimitrisli.wordpress.com/2011/03/06/junit-showcase-setting-up-and-optimising-unit-tests/
public class BeforeAfterTest_wildcardImport_Post {

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
