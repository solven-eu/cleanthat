package eu.solven.cleanthat.mvn;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class TestCleanThatCleanThatMojoTest extends ACleanThatMojoTest {
	@Test
	public void testCleanthat() throws Exception {
		String relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		// Ensure the test resources does not hold a cleanthat.yaml
		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		Resource defaultConfig = new ClassPathResource("/config/default-safe.yaml");
		Files.copy(defaultConfig.getInputStream(), cleanthatYaml.toPath(), StandardCopyOption.REPLACE_EXISTING);

		CleanThatCleanThatMojo myMojo = (CleanThatCleanThatMojo) lookupConfiguredMojo(project, "cleanthat");
		assertNotNull(myMojo);

		myMojo.execute();
	}
}