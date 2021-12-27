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
