import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import eu.solven.cleanthat.mvn.CleanThatCleanThatMojo;

public class MyMojoTest extends AbstractMojoTestCase {
	/** {@inheritDoc} */
	protected void setUp() throws Exception {
		// required
		super.setUp();

		// ...
	}

	/** {@inheritDoc} */
	protected void tearDown() throws Exception {
		// required
		super.tearDown();

		// ...
	}

	/**
	 * @throws Exception
	 *             if any
	 */
	public void testSomething() throws Exception {
		File pom = getTestFile("src/test/resources/unit/project-to-test/pom.xml");
		assertNotNull(pom);
		assertTrue(pom.exists());

		CleanThatCleanThatMojo myMojo = (CleanThatCleanThatMojo) lookupMojo("cleanthat", pom);
		assertNotNull(myMojo);
		myMojo.execute();

		// ...
	}
}