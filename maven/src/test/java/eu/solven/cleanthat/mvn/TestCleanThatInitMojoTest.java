/*
 * Copyright 2023 Solven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.mvn;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import java.io.File;
import java.util.Arrays;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

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