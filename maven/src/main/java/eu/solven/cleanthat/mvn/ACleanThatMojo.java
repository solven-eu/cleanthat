package eu.solven.cleanthat.mvn;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * The mojo of the mvn plugin
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
public abstract class ACleanThatMojo extends AbstractMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatInitMojo.class);

	// http://maven.apache.org/ref/3.1.1/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	private MavenSession session;

	// https://stackoverflow.com/questions/3084629/finding-the-root-directory-of-a-multi-module-maven-reactor-project
	@Parameter(property = "cleanthat.configPath", defaultValue = "${maven.multiModuleProjectDirectory}/cleanthat.yaml")
	private String configPath;

	@Parameter(property = "cleanthat.configUrl")
	private String configUrl;

	// Useful to check what are the expected impacts without actually changing project files
	@Parameter(property = "cleanthat.dryRun", defaultValue = "false")
	private boolean dryRun;

	/**
	 * Base directory of the project.
	 */
	// https://github.com/khmarbaise/maven-assembly-plugin/blob/master/src/main/java/org/apache/maven/plugin/assembly/mojos/AbstractAssemblyMojo.java#L214
	// Is this the same as 'getProject().getBasedir()'?
	@Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
	private File basedir;

	/**
	 * Runs the plugin only if the current project is the execution root.
	 *
	 * This is helpful, if the plugin is defined in a profile and should only run once to download a shared file.
	 */
	// https://github.com/maven-download-plugin/maven-download-plugin/blob/master/src/main/java/com/googlecode/download/maven/plugin/internal/WGet.java
	// https://github.com/khmarbaise/maven-assembly-plugin/blob/master/src/main/java/org/apache/maven/plugin/assembly/mojos/AbstractAssemblyMojo.java#L335
	@Parameter(property = "runOnlyAtRoot", defaultValue = "false")
	private boolean runOnlyAtRoot;

	protected void checkParameters() {
		String configPath = getConfigPath();
		String configUrl = getConfigUrl();

		if (Strings.isNullOrEmpty(configPath) && Strings.isNullOrEmpty(configUrl)) {
			throw new IllegalStateException("configPath and configUrl are both empty");
		} else if (!Strings.isNullOrEmpty(configPath) && !Strings.isNullOrEmpty(configUrl)
				&& !configPath.equals(configUrl)) {
			throw new IllegalStateException(
					"configPath and configUrl are both non-empty: " + configPath + " vs " + configUrl);
		}
	}

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

	public boolean isDryRun() {
		return dryRun;
	}

	public boolean isRunOnlyAtRoot() {
		return runOnlyAtRoot;
	}

	/**
	 * Returns true if the current project is located at the Execution Root Directory (where mvn was launched)
	 * 
	 * @return
	 */
	// https://blog.sonatype.com/2009/05/how-to-make-a-plugin-run-once-during-a-build/
	protected boolean isThisTheExecutionRoot() {
		LOGGER.debug("Root Folder: {}", session.getExecutionRootDirectory());
		LOGGER.debug("Current Folder: {}", basedir);
		boolean result = session.getExecutionRootDirectory().equalsIgnoreCase(basedir.toString());
		if (result) {
			LOGGER.debug("This is the execution root.");
			if (!getProject().isExecutionRoot()) {
				LOGGER.warn("Unclear if this is the executionRoot: {} vs {}", basedir, getProject());
			}
		} else {
			LOGGER.debug("This is NOT the execution root.");
			if (getProject().isExecutionRoot()) {
				LOGGER.warn("Unclear if this is the executionRoot: {} vs {}", basedir, getProject());
			}
		}
		return result;
	}

}