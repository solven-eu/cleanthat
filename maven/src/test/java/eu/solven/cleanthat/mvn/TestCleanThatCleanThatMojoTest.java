package eu.solven.cleanthat.mvn;

import java.io.File;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.Test;

public class TestCleanThatCleanThatMojoTest extends AbstractMojoTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// ...
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		// ...
	}

	/**
	 * @throws Exception
	 *             if any
	 */
	@Test
	public void testSomething() throws Exception {
		getContainer().getComponentDescriptorMap(Mojo.class.getName());

		File pom = getTestFile("src/test/resources/unit/project-to-test/pom.xml");
		assertNotNull(pom);
		assertTrue(pom.exists());

		// https://stackoverflow.com/questions/9496534/test-default-values-and-expressions-of-mojos-using-maven-plugin-testing-harness
		MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
		// executionRequest.setRepo
		ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
		buildingRequest.setRepositorySession(new DefaultRepositorySystemSession());
		ProjectBuilder projectBuilder = this.lookup(ProjectBuilder.class);
		MavenProject project = projectBuilder.build(pom, buildingRequest).getProject();

		CleanThatCleanThatMojo myMojo = (CleanThatCleanThatMojo) lookupConfiguredMojo(project, "cleanthat");
		assertNotNull(myMojo);

		myMojo.execute();

		// ...
	}
}