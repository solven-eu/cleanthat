/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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

import java.io.File;
import java.util.Arrays;

import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import eu.solven.cleanthat.code_provider.local.FileSystemGitCodeProvider;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ICleanthatConfigConstants;
import eu.solven.cleanthat.language.spotless.CleanthatSpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessEngineProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import junit.framework.AssertionFailedError;

public class TestCleanThatInitMojoTest extends ACleanThatMojoTest {

	final ObjectMapper objectMapper = ConfigHelpers.makeYamlObjectMapper();

	@Test
	public void testInit_noPom() throws Exception {
		var relativePathToParent = "/unit/no_pom.xml";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		Assertions.assertThatThrownBy(() -> prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder))
				.isInstanceOf(AssertionFailedError.class);
	}

	@Test
	public void testInit_onlyPom() throws Exception {
		var relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatInitMojo myMojo = (CleanThatInitMojo) lookupConfiguredMojo(project, CleanThatInitMojo.MOJO_INIT);
		assertNotNull(myMojo);

		var cleanthatYaml = readWriteFolder.toPath().resolve(ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT).toFile();
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();
		var configHelpers = new ConfigHelpers(Arrays.asList(objectMapper));
		var config = configHelpers.loadRepoConfig(new FileSystemResource(cleanthatYaml));

		Assert.assertEquals("2023-01-09", config.getSyntaxVersion());
		Assert.assertEquals(1, config.getEngines().size());
		Assert.assertEquals("spotless", config.getEngines().get(0).getEngine());

		Assertions.assertThat(config.getEngines().get(0).getSteps()).hasSize(1);
		var singleStep = config.getEngines().get(0).getSteps().get(0);

		var stepParameters =
				objectMapper.convertValue(singleStep.getParameters(), CleanthatSpotlessStepParametersProperties.class);
		var url = stepParameters.getConfiguration();

		SpotlessEngineProperties spotlessConfigResource = CleanThatGenerateEclipseStylesheetMojo
				.loadSpotlessEngineProperties(new FileSystemGitCodeProvider(readWriteFolder.toPath()),
						objectMapper,
						url);

		Assert.assertEquals(2, spotlessConfigResource.getFormatters().size());
		{

			SpotlessFormatterProperties formatter = spotlessConfigResource.getFormatters().get(0);
			Assert.assertEquals("markdown", formatter.getFormat());

			Assert.assertEquals(2, formatter.getSteps().size());

			Assert.assertEquals("flexmark", Iterables.getFirst(formatter.getSteps(), null).getId());
			Assert.assertEquals("freshmark", Iterables.getLast(formatter.getSteps()).getId());
		}
		{
			SpotlessFormatterProperties formatter = spotlessConfigResource.getFormatters().get(1);
			Assert.assertEquals("pom", formatter.getFormat());

			Assert.assertEquals(1, formatter.getSteps().size());

			SpotlessStepProperties firstStep = formatter.getSteps().get(0);
			Assert.assertEquals("sortPom", firstStep.getId());
		}
	}

	@Test
	public void testInit_javaJson() throws Exception {
		var relativePathToParent = "/unit/java-json";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatInitMojo myMojo = (CleanThatInitMojo) lookupConfiguredMojo(project, CleanThatInitMojo.MOJO_INIT);
		assertNotNull(myMojo);

		var cleanthatYaml = readWriteFolder.toPath().resolve(ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT).toFile();
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();
		var configHelpers = new ConfigHelpers(Arrays.asList(objectMapper));
		var config = configHelpers.loadRepoConfig(new FileSystemResource(cleanthatYaml));

		Assert.assertEquals("2023-01-09", config.getSyntaxVersion());
		Assert.assertEquals(2, config.getEngines().size());
		Assert.assertEquals("spotless", config.getEngines().get(0).getEngine());
		Assert.assertEquals("openrewrite", config.getEngines().get(1).getEngine());

		Assertions.assertThat(config.getEngines().get(0).getSteps()).hasSize(1);
		var singleStep = config.getEngines().get(0).getSteps().get(0);

		var stepParameters =
				objectMapper.convertValue(singleStep.getParameters(), CleanthatSpotlessStepParametersProperties.class);
		var url = stepParameters.getConfiguration();

		SpotlessEngineProperties spotlessConfigResource = CleanThatGenerateEclipseStylesheetMojo
				.loadSpotlessEngineProperties(new FileSystemGitCodeProvider(readWriteFolder.toPath()),
						objectMapper,
						url);

		Assert.assertEquals(3, spotlessConfigResource.getFormatters().size());
		{

			SpotlessFormatterProperties formatter = spotlessConfigResource.getFormatters().get(0);
			Assert.assertEquals("java", formatter.getFormat());

			Assert.assertEquals(5, formatter.getSteps().size());

			Assert.assertEquals("toggleOffOn", Iterables.getFirst(formatter.getSteps(), null).getId());
			Assert.assertEquals("cleanthat", Iterables.get(formatter.getSteps(), 1).getId());
			Assert.assertEquals("eclipse", Iterables.getLast(formatter.getSteps()).getId());
		}
		{

			SpotlessFormatterProperties formatter = spotlessConfigResource.getFormatters().get(1);
			Assert.assertEquals("json", formatter.getFormat());

			Assert.assertEquals(1, formatter.getSteps().size());

			SpotlessStepProperties firstStep = formatter.getSteps().get(0);
			Assert.assertEquals("jackson", firstStep.getId());
		}
		{
			SpotlessFormatterProperties formatter = spotlessConfigResource.getFormatters().get(2);
			Assert.assertEquals("pom", formatter.getFormat());

			Assert.assertEquals(1, formatter.getSteps().size());

			SpotlessStepProperties firstStep = formatter.getSteps().get(0);
			Assert.assertEquals("sortPom", firstStep.getId());
		}
		// {
		//
		// SpotlessFormatterProperties formatter = spotlessConfigResource.getFormatters().get(3);
		// Assert.assertEquals("yaml", formatter.getFormat());
		//
		// Assert.assertEquals(1, formatter.getSteps().size());
		//
		// SpotlessStepProperties firstStep = formatter.getSteps().get(0);
		// Assert.assertEquals("jackson", firstStep.getId());
		// }
	}
}