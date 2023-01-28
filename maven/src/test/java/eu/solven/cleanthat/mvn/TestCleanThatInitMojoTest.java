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

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.language.spotless.CleanthatSpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessEngineProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import java.io.File;
import java.util.Arrays;
import junit.framework.AssertionFailedError;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

public class TestCleanThatInitMojoTest extends ACleanThatMojoTest {

	final ObjectMapper objectMapper = ConfigHelpers.makeYamlObjectMapper();

	@Test
	public void testInit_noPom() throws Exception {
		String relativePathToParent = "/unit/no_pom.xml";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		Assertions.assertThatThrownBy(() -> prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder))
				.isInstanceOf(AssertionFailedError.class);
	}

	@Test
	public void testInit_onlyPom() throws Exception {
		String relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatInitMojo myMojo = (CleanThatInitMojo) lookupConfiguredMojo(project, CleanThatInitMojo.MOJO_INIT);
		assertNotNull(myMojo);

		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();
		ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(objectMapper));
		CleanthatRepositoryProperties config = configHelpers.loadRepoConfig(new FileSystemResource(cleanthatYaml));

		Assert.assertEquals("2023-01-09", config.getSyntaxVersion());
		Assert.assertEquals(1, config.getEngines().size());
		Assert.assertEquals("spotless", config.getEngines().get(0).getEngine());

		Assertions.assertThat(config.getEngines().get(0).getSteps()).hasSize(1);
		CleanthatStepProperties singleStep = config.getEngines().get(0).getSteps().get(0);

		CleanthatSpotlessStepParametersProperties stepParameters =
				objectMapper.convertValue(singleStep.getParameters(), CleanthatSpotlessStepParametersProperties.class);
		String url = stepParameters.getConfiguration();

		SpotlessEngineProperties spotlessConfigResource = CleanThatGenerateEclipseStylesheetMojo
				.loadSpotlessEngineProperties(new FileSystemCodeProvider(readWriteFolder.toPath()), objectMapper, url);

		// TODO We should add spotlessSteps dynamically
		Assert.assertEquals(1, spotlessConfigResource.getFormatters().size());
		{

			SpotlessFormatterProperties formatter = spotlessConfigResource.getFormatters().get(0);
			Assert.assertEquals("markdown", formatter.getFormat());

			Assert.assertEquals(1, formatter.getSteps().size());

			SpotlessStepProperties firstStep = formatter.getSteps().get(0);
			Assert.assertEquals("flexmark", firstStep.getId());
		}
		// {
		// SpotlessFormatterProperties formatter = spotlessConfigResource.getFormatters().get(1);
		// Assert.assertEquals("pomXml", formatter.getFormat());
		//
		// Assert.assertEquals(1, formatter.getSteps().size());
		//
		// SpotlessStepProperties firstStep = formatter.getSteps().get(0);
		// Assert.assertEquals("sortPom", firstStep.getId());
		// }
	}

	@Test
	public void testInit_javaJson() throws Exception {
		String relativePathToParent = "/unit/java-json";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatInitMojo myMojo = (CleanThatInitMojo) lookupConfiguredMojo(project, CleanThatInitMojo.MOJO_INIT);
		assertNotNull(myMojo);

		File cleanthatYaml = new File(readWriteFolder, "cleanthat.yaml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();
		ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(objectMapper));
		CleanthatRepositoryProperties config = configHelpers.loadRepoConfig(new FileSystemResource(cleanthatYaml));

		Assert.assertEquals("2023-01-09", config.getSyntaxVersion());
		Assert.assertEquals(1, config.getEngines().size());
		Assert.assertEquals("spotless", config.getEngines().get(0).getEngine());

		Assertions.assertThat(config.getEngines().get(0).getSteps()).hasSize(1);
		CleanthatStepProperties singleStep = config.getEngines().get(0).getSteps().get(0);

		CleanthatSpotlessStepParametersProperties stepParameters =
				objectMapper.convertValue(singleStep.getParameters(), CleanthatSpotlessStepParametersProperties.class);
		String url = stepParameters.getConfiguration();

		SpotlessEngineProperties spotlessConfigResource = CleanThatGenerateEclipseStylesheetMojo
				.loadSpotlessEngineProperties(new FileSystemCodeProvider(readWriteFolder.toPath()), objectMapper, url);

		// TODO We should add spotlessSteps dynamically
		Assert.assertEquals(1, spotlessConfigResource.getFormatters().size());
		{

			SpotlessFormatterProperties formatter = spotlessConfigResource.getFormatters().get(0);
			Assert.assertEquals("markdown", formatter.getFormat());

			Assert.assertEquals(1, formatter.getSteps().size());

			SpotlessStepProperties firstStep = formatter.getSteps().get(0);
			Assert.assertEquals("flexmark", firstStep.getId());
		}

		// {
		// SpotlessStepProperties step = firstFormatter.getSteps().get(0);
		// Assert.assertEquals("java", step.getId());
		// }
		// {
		// SpotlessStepProperties step = firstFormatter.getSteps().get(1);
		// Assert.assertEquals("json", step.getId());
		// }
		// {
		// SpotlessStepProperties step = firstFormatter.getSteps().get(2);
		// Assert.assertEquals("xml", step.getId());
		// }
	}
}