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
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.springframework.util.FileSystemUtils;

public abstract class ACleanThatMojoTest extends AbstractMojoTestCase {

	public File temporaryFolder(String relativePathToParent) {
		File temporaryFolder = getTestFile("target/test-classes" + relativePathToParent);

		if (!FileSystemUtils.deleteRecursively(temporaryFolder)) {
			if (temporaryFolder.exists()) {
				throw new IllegalArgumentException("Issue deleting: " + temporaryFolder);
			}
		}

		return temporaryFolder;
	}

	public MavenProject prepareMojoInTemporaryFolder(String relativePathToParent, File readWriteFolder)
			throws IOException, ComponentLookupException, ProjectBuildingException {
		File readOnlyFolder = getTestFile("src/test/resources" + relativePathToParent);

		// As we will generate file, we move to a temporary location
		FileUtils.copyDirectory(readOnlyFolder, readWriteFolder);

		// We need a pom to execute the Mojo (i.e. it is difficult to test the markdown-only case)
		File pom = new File(readWriteFolder, "pom.xml");
		assertNotNull(pom);
		assertTrue(pom.exists());

		// https://stackoverflow.com/questions/9496534/test-default-values-and-expressions-of-mojos-using-maven-plugin-testing-harness
		MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();

		{
			Properties userProperties = new Properties();
			// Unclear why this is not setup by default test-harness
			userProperties.setProperty("maven.multiModuleProjectDirectory", readWriteFolder.getAbsolutePath());
			executionRequest.setUserProperties(userProperties);
		}

		ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
		buildingRequest.setRepositorySession(new DefaultRepositorySystemSession());
		ProjectBuilder projectBuilder = this.lookup(ProjectBuilder.class);
		MavenProject project = projectBuilder.build(pom, buildingRequest).getProject();
		return project;
	}

	@Override
	protected Mojo lookupConfiguredMojo(MavenProject project, String goal) throws Exception {
		MavenSession session = newMavenSession(project);

		// This enables the use of ${session.executionRootDirectory}
		session.getRequest().setBaseDirectory(project.getBasedir());
		// This enables the use of ${maven.multiModuleProjectDirectory}
		session.getRequest().setMultiModuleProjectDirectory(project.getBasedir());

		// This enables adding -Dxxx.yyy=zzz
		// It is especially useful to force ${maven.multiModuleProjectDirectory} in tests
		session.getRequest().setUserProperties(project.getProjectBuildingRequest().getUserProperties());

		return lookupConfiguredMojo(session, newMojoExecution(goal));
	}

	protected CleanThatInitMojo lookupConfiguredInitMojo(MavenProject project) {
		try {
			return (CleanThatInitMojo) lookupConfiguredMojo(project, CleanThatInitMojo.MOJO_INIT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected CleanThatCleanThatMojo lookupConfiguredFixMojo(MavenProject project) {
		try {
			return (CleanThatCleanThatMojo) lookupConfiguredMojo(project, CleanThatCleanThatMojo.MOJO_FIX);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected CleanThatCheckMojo lookupConfiguredCheckMojo(MavenProject project) {
		try {
			return (CleanThatCheckMojo) lookupConfiguredMojo(project, CleanThatCheckMojo.MOJO_CHECK);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}