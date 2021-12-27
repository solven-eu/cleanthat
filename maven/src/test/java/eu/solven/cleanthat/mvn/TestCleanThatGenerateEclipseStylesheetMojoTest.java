package eu.solven.cleanthat.mvn;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestCleanThatGenerateEclipseStylesheetMojoTest extends ACleanThatMojoTest {

	@Test
	public void testGenerateEclipse() throws Exception {
		String relativePathToParent = "/unit/with-git-ignore";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatGenerateEclipseStylesheetMojo myMojo =
				(CleanThatGenerateEclipseStylesheetMojo) lookupConfiguredMojo(project,
						CleanThatGenerateEclipseStylesheetMojo.GOAL_ECLIPSE);
		assertNotNull(myMojo);

		File cleanthatYaml = new File(readWriteFolder, ".cleanthat/eclipse_formatter-stylesheet.xml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();
	}
}