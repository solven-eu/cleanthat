package eu.solven.cleanthat.mvn;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestCleanThatInitMojoTest extends ACleanThatMojoTest {

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