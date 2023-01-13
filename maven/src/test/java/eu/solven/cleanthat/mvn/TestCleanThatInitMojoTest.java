package eu.solven.cleanthat.mvn;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.config.pojo.LanguageProperties;

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
		Assert.assertEquals(1, config.getLanguages().size());
		Assert.assertEquals("xml", config.getLanguages().get(0).getLanguage());
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
		Assertions.assertThat(config.getLanguages()).hasSize(3);

		{
			LanguageProperties languageProperties = config.getLanguages().get(0);

			Assert.assertEquals("java", languageProperties.getLanguage());
		}
		{
			LanguageProperties languageProperties = config.getLanguages().get(1);

			Assert.assertEquals("json", languageProperties.getLanguage());
		}
		{
			LanguageProperties languageProperties = config.getLanguages().get(2);

			Assert.assertEquals("xml", languageProperties.getLanguage());
		}
	}
}