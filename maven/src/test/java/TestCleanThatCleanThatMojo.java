import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Ignore;

import eu.solven.cleanthat.mvn.CleanThatCleanThatMojo;

@Ignore("ProjetModel is not injected")
public class TestCleanThatCleanThatMojo extends AbstractMojoTestCase {
	protected void setUp() throws Exception {
		super.setUp();

		// ...
	}

	protected void tearDown() throws Exception {
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