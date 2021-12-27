package eu.solven.cleanthat.mvn;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("TODO It is unclear how one can test a Mojo without an underlying pom")
public class TestCleanThatInitMojoNoPomTest extends ACleanThatMojoTest {

	@Ignore("TODO It is unclear how one can test a Mojo without an underlying pom")
	@Test
	public void testInit() throws Exception {
		String relativePathToParent = "/unit/no_pom.xml";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatInitMojo myMojo = (CleanThatInitMojo) lookupConfiguredMojo(project, "init");
		assertNotNull(myMojo);

		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();
	}

	@Ignore("TODO It is unclear how one can test a Mojo without an underlying pom")
	@Test
	public void testInit2() throws Exception {
		String relativePathToParent = "/unit/no_pom.xml";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MojoExecution mojoExec = newMojoExecution("init");

		MojoDescriptor mojoDescriptor = mojoExec.getMojoDescriptor();

		CleanThatInitMojo myMojo = (CleanThatInitMojo) lookup(mojoDescriptor.getRole(), mojoDescriptor.getRoleHint());
		assertNotNull(myMojo);

		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();
	}
}