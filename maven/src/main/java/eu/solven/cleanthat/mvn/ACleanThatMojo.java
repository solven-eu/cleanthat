package eu.solven.cleanthat.mvn;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The mojo of the mvn plugin
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
public abstract class ACleanThatMojo extends AbstractMojo {
	@Parameter(property = "cleanthat.configPath", defaultValue = "${project.basedir}/cleanthat.yaml")
	private String configPath;

	@Parameter(property = "cleanthat.configUrl")
	private String configUrl;

	public String getConfigPath() {
		return configPath;
	}

	public String getConfigUrl() {
		return configUrl;
	}
}