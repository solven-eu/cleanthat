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

import org.apache.maven.project.MavenProject;
import org.junit.Test;

public class TestCleanThatApplyMojoTest extends ACleanThatMojoTest {
	@Test
	public void testCleanthat_noConfig() throws Exception {
		var relativePathToParent = "/unit/project-to-test";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatApplyMojo myMojo = (CleanThatApplyMojo) lookupConfiguredMojo(project, CleanThatApplyMojo.MOJO_SINGLE);

		myMojo.execute();
	}

	@Test
	public void testCleanthat_javaJson() throws Exception {
		var relativePathToParent = "/unit/java-json";
		File readWriteFolder = temporaryFolder(relativePathToParent);

		MavenProject project = prepareMojoInTemporaryFolder(relativePathToParent, readWriteFolder);

		CleanThatApplyMojo myMojo = (CleanThatApplyMojo) lookupConfiguredMojo(project, CleanThatApplyMojo.MOJO_SINGLE);

		myMojo.execute();
	}
}