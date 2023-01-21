package eu.solven.cleanthat.mvn;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;

public class TestCleanThatInitMojoTest extends ACleanThatMojoTest {

	@Test
	public void testInit_onlyPom() throws Exception {
		String relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatInitMojo myMojo = (CleanThatInitMojo) lookupConfiguredMojo(project, "init");
		assertNotNull(myMojo);

		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();

		ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(ConfigHelpers.makeYamlObjectMapper()));
		CleanthatRepositoryProperties config = configHelpers.loadRepoConfig(new FileSystemResource(cleanthatYaml));

		Assert.assertEquals("2021-08-02", config.getSyntaxVersion());
		Assert.assertEquals(1, config.getEngines().size());
		Assert.assertEquals("xml", config.getEngines().get(0).getEngine());
	}

	@Test
	public void testInit_javaJson() throws Exception {
		String relativePathToParent = "/unit/java-json";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatInitMojo myMojo = (CleanThatInitMojo) lookupConfiguredMojo(project, "init");
		assertNotNull(myMojo);

		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();

		ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(ConfigHelpers.makeYamlObjectMapper()));
		CleanthatRepositoryProperties config = configHelpers.loadRepoConfig(new FileSystemResource(cleanthatYaml));

		Assert.assertEquals("2021-08-02", config.getSyntaxVersion());
		Assertions.assertThat(config.getEngines()).hasSize(3);

		{
			CleanthatEngineProperties languageProperties = config.getEngines().get(0);

			Assert.assertEquals("java", languageProperties.getEngine());
		}
		{
			CleanthatEngineProperties languageProperties = config.getEngines().get(1);

			Assert.assertEquals("json", languageProperties.getEngine());
		}
		{
			CleanthatEngineProperties languageProperties = config.getEngines().get(2);

			Assert.assertEquals("xml", languageProperties.getEngine());
		}
	}
}