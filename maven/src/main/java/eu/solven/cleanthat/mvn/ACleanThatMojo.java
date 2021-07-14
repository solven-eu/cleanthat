package eu.solven.cleanthat.mvn;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * The mojo of the mvn plugin
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
public abstract class ACleanThatMojo extends AbstractMojo {
	// http://maven.apache.org/ref/3.1.1/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	private MavenSession session;

	@Parameter(property = "cleanthat.configPath", defaultValue = "${project.basedir}/cleanthat.yaml")
	private String configPath;

	@Parameter(property = "cleanthat.configUrl")
	private String configUrl;

	public MavenProject getProject() {
		return project;
	}

	public MavenSession getSession() {
		return session;
	}

	public String getConfigPath() {
		return configPath;
	}

	public String getConfigUrl() {
		return configUrl;
	}
}