package eu.solven.cleanthat.mvn;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.assertj.core.api.Assertions;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

public class TestCleanThatInitMojoTest extends AbstractMojoTestCase {

	public File temporaryFolder(String relativePathToParent) {
		File temporaryFolder = getTestFile("target/test-classes" + relativePathToParent);

		if (!FileSystemUtils.deleteRecursively(temporaryFolder)) {
			throw new IllegalArgumentException("Issue deleting: " + temporaryFolder);
		}

		return temporaryFolder;
	}

	public MavenProject prepareMojoInTemporaryFolder(String relativePathToParent, File readWriteFolder)
			throws IOException, ComponentLookupException, ProjectBuildingException {
		File readOnlyFolder = getTestFile("src/test/resources" + relativePathToParent);

		// As we will generate file, we move to a temporary location
		FileUtils.copyDirectory(readOnlyFolder, readWriteFolder);

		File pom = new File(readWriteFolder, "pom.xml");
		assertNotNull(pom);
		assertTrue(pom.exists());

		// https://stackoverflow.com/questions/9496534/test-default-values-and-expressions-of-mojos-using-maven-plugin-testing-harness
		MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
		ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
		buildingRequest.setRepositorySession(new DefaultRepositorySystemSession());
		ProjectBuilder projectBuilder = this.lookup(ProjectBuilder.class);
		MavenProject project = projectBuilder.build(pom, buildingRequest).getProject();
		return project;
	}

	@Test
	public void testInit() throws Exception {
		String relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatInitMojo myMojo = (CleanThatInitMojo) lookupConfiguredMojo(project, "init");
		assertNotNull(myMojo);

		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();
	}
}