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

		File cleanthatYaml = new File(readWriteFolder, ".cleanthat/eclipse_java-stylesheet.xml");
		Assertions.assertThat(cleanthatYaml).doesNotExist();

		myMojo.execute();

		Assertions.assertThat(cleanthatYaml).isFile();
	}
}