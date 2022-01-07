package eu.solven.cleanthat.mvn;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.boot.SpringApplication;

import cormoran.pepper.unittest.ILogDisabler;
import cormoran.pepper.unittest.PepperTestHelper;

public class TestCleanThatCleanThatMojoTest extends ACleanThatMojoTest {
	@Test
	public void testCleanthat_noConfig() throws Exception {
		String relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		// Ensure the test resources does not hold a cleanthat.yaml
		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatCleanThatMojo myMojo = (CleanThatCleanThatMojo) lookupConfiguredMojo(project, "cleanthat");

		try (ILogDisabler logCLoser = PepperTestHelper.disableLog(SpringApplication.class)) {
			Assertions.assertThatThrownBy(() -> myMojo.execute()).isInstanceOf(IllegalStateException.class);
		}
	}

	@Test
	public void testCleanthat_initThenLint() throws Exception {
		String relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		// Ensure the test resources does not hold a cleanthat.yaml
		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		// 'init' will create a cleanthat.yaml
		CleanThatInitMojo initMojo = lookupConfiguredInitMojo(project);
		initMojo.execute();

		CleanThatCleanThatMojo fixMojo = lookupConfiguredFixMojo(project);
		fixMojo.execute();
	}
}