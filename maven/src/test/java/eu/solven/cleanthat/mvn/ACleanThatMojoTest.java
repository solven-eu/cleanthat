package eu.solven.cleanthat.mvn;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.springframework.util.FileSystemUtils;

public abstract class ACleanThatMojoTest extends AbstractMojoTestCase {

	public File temporaryFolder(String relativePathToParent) {
		File temporaryFolder = getTestFile("target/test-classes" + relativePathToParent);

		if (!FileSystemUtils.deleteRecursively(temporaryFolder)) {
			if (temporaryFolder.exists()) {
				throw new IllegalArgumentException("Issue deleting: " + temporaryFolder);
			}
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

	@Override
	protected Mojo lookupConfiguredMojo(MavenProject project, String goal) throws Exception {
		MavenSession session = newMavenSession(project);

		// This enable the use of ${session.executionRootDirectory}
		session.getRequest().setBaseDirectory(project.getBasedir());

		return lookupConfiguredMojo(session, newMojoExecution(goal));
	}
}