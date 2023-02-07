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

import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class TestACleanThatMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestACleanThatMojo.class);

	final ACleanThatMojo mojo = new ACleanThatMojo() {

		@Override
		public void execute() throws MojoExecutionException, MojoFailureException {
			LOGGER.debug("Called .execute()");
		}
	};

	@Test
	public void testBaseUriWithPomXml() throws IOException {
		MavenProject project = new MavenProject();
		project.setFile(new ClassPathResource("/unit/java-json/pom.xml").getFile());
		Assertions.assertThat(project.getBasedir()).hasName("java-json");

		mojo.setProject(project);

		Assertions.assertThat(mojo.getBaseDir()).hasName("java-json");
	}

	@Test
	public void testBaseUriNoPomXml() throws IOException {
		MavenProject project = new MavenProject();
		mojo.setProject(project);

		MavenSession session = Mockito.mock(MavenSession.class);
		Mockito.when(session.getExecutionRootDirectory()).thenReturn("/some/folder");
		mojo.setSession(session);

		Assertions.assertThat(mojo.getBaseDir()).hasName("folder");
	}
}
