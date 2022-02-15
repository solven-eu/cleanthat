package eu.solven.cleanthat.language.java.rules.cases.junit4tojunit5;

import org.junit.*;

// https://dimitrisli.wordpress.com/2011/03/06/junit-showcase-setting-up-and-optimising-unit-tests/
public class BeforeAfterTest_wildcardImport_Pre {

	@BeforeClass
	public static void initialiseClass() {
		System.out.println("init class");
	}

	@Before
	public void initialiseTest() {
		System.out.println("init test");
	}

	@Test
	public void test1() {
		System.out.println("inside test1");
		Assert.assertTrue(true);
	}

	@Test
	public void test2() {
		System.out.println("inside test2");
		Assert.assertTrue(true);
	}

	@After
	public void teardownTest() {
		System.out.println("teardown test");
	}

	@AfterClass
	public static void teardownClass() {
		System.out.println("teardown class");
	}
}