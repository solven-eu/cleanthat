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
import java.nio.file.FileSystem;
import java.nio.file.Files;

import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.boot.SpringApplication;

import com.google.common.jimfs.Jimfs;

import eu.solven.cleanthat.config.ICleanthatConfigConstants;
import eu.solven.pepper.unittest.ILogDisabler;
import eu.solven.pepper.unittest.PepperTestHelper;

public class TestCleanThatCheckMojoTest extends ACleanThatMojoTest {
	final FileSystem fs = Jimfs.newFileSystem();

	@Test
	public void testCleanthat_noConfig() throws Exception {
		String relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		// Ensure the test resources does not hold a cleanthat.yaml
		var cleanthatYaml = readWriteFolder.toPath().resolve(ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT).toFile();
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatCleanThatMojo myMojo = lookupConfiguredFixMojo(project);
		myMojo.setFileSystem(fs);

		try (ILogDisabler logCLoser = PepperTestHelper.disableLog(SpringApplication.class)) {
			Assertions.assertThatThrownBy(() -> myMojo.execute()).isInstanceOf(IllegalStateException.class);
		}
	}

	@Test
	public void testCleanthat_initThenCheck() throws Exception {
		String relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		// Ensure the test resources does not hold a cleanthat.yaml
		var cleanthatYaml = readWriteFolder.toPath().resolve(ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT).toFile();
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		// 'init' will create a cleanthat.yaml
		CleanThatInitMojo initMojo = lookupConfiguredInitMojo(project);

		String executionRootDirectory = initMojo.getSession().getExecutionRootDirectory();

		initMojo.setFileSystem(fs);
		Files.createDirectories(fs.getPath(executionRootDirectory));

		initMojo.execute();

		CleanThatCheckMojo checkMojo = lookupConfiguredCheckMojo(project);
		checkMojo.setFileSystem(fs);
		checkMojo.execute();
	}

	@Test
	public void testCleanthat_initThenLintThenCheck() throws Exception {
		String relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		// Ensure the test resources does not hold a cleanthat.yaml
		var cleanthatYaml = readWriteFolder.toPath().resolve(ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT).toFile();
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		// 'init' will create a cleanthat.yaml
		CleanThatInitMojo initMojo = lookupConfiguredInitMojo(project);
		String executionRootDirectory = initMojo.getSession().getExecutionRootDirectory();
		Files.createDirectories(fs.getPath(executionRootDirectory));
		initMojo.setFileSystem(fs);
		initMojo.execute();

		CleanThatCleanThatMojo cleanthatMojo = lookupConfiguredFixMojo(project);
		cleanthatMojo.setFileSystem(fs);
		cleanthatMojo.execute();

		CleanThatCheckMojo checkMojo = lookupConfiguredCheckMojo(project);
		checkMojo.setFileSystem(fs);
		checkMojo.execute();
	}
}