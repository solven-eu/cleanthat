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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
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
	// It seems not possible to rely on 'maven.XXX' (hence not '${maven.multiModuleProjectDirectory}') in plugin
	// parameters. See PluginParameterExpressionEvaluator
	// @Parameter(property = "cleanthat.configPath", defaultValue = "${session.basedir}/cleanthat.yaml")
	// @Parameter(property = "cleanthat.configPath", defaultValue = "${project.basedir}/cleanthat.yaml")
	// The following is not compatible with goals executable without a pom.xml
	// @Parameter(property = "cleanthat.configPath", defaultValue = "${session.topLevelProject.basedir}/cleanthat.yaml")
	// The following is not compatible with UnitTests
	@Parameter(property = "cleanthat.configPath", defaultValue = "${session.executionRootDirectory}/cleanthat.yaml")
	private String configPath;

	@Parameter(property = "cleanthat.configUrl")
	private String configUrl;

	// Useful to check what are the expected impacts without actually changing project files
	@Parameter(property = "cleanthat.dryRun", defaultValue = "false")
	private boolean dryRun;

	@Parameter(property = "cleanthat.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * Base directory of the project.
	 */
	// https://github.com/khmarbaise/maven-assembly-plugin/blob/master/src/main/java/org/apache/maven/plugin/assembly/mojos/AbstractAssemblyMojo.java#L214
	// Is this the same as 'getProject().getBasedir()'?
	// @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
	// private File basedir;

	/**
	 * Runs the plugin only if the current project is the execution root.
	 *
	 * This is helpful, if the plugin is defined in a profile and should only run once to download a shared file.
	 */
	// https://github.com/maven-download-plugin/maven-download-plugin/blob/master/src/main/java/com/googlecode/download/maven/plugin/internal/WGet.java
	// https://github.com/khmarbaise/maven-assembly-plugin/blob/master/src/main/java/org/apache/maven/plugin/assembly/mojos/AbstractAssemblyMojo.java#L335
	@Parameter(property = "runOnlyAtRoot", defaultValue = "false")
	private boolean runOnlyAtRoot;

	@VisibleForTesting
	protected void setProject(MavenProject project) {
		this.project = project;
	}

	public MavenProject getProject() {
		return project;
	}

	@VisibleForTesting
	protected void setSession(MavenSession session) {
		this.session = session;
	}

	public MavenSession getSession() {
		return session;
	}

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

	public File getBaseDir() {
		// The baseDir is the project being processed
		// In a multi-module, we want to process the folder containing the targetted pom.xml
		// It may not be the root pom.xml, if one targetted specifically a child module
		File baseDir = getProject().getBasedir();

		if (baseDir == null) {
			// We are processing a folder with no pom.xml
			baseDir = new File(getSession().getExecutionRootDirectory());
			LOGGER.info("Current folder has no pom.xml");
			LOGGER.info("Consider as baseDir: {}", baseDir);
		}
		getLog().info("baseDir: " + baseDir);
		return baseDir;
	}

	public String getConfigPath() {
		if (configPath != null && configPath.contains("${")) {
			// session.get
			// project.getBasedir();
			// session.getExecutionRootDirectory();
			// session.getBas
			throw new IllegalStateException("Issue with configPath: '" + configPath + "'");
		}

		return configPath;
	}

	@VisibleForTesting
	public void setConfigUrl(String configUrl) {
		this.configUrl = configUrl;
	}

	public String getConfigUrl() {
		if (configUrl != null && configUrl.contains("${")) {
			throw new IllegalStateException("Issue with configUrl: '" + configUrl + "'");
		}
		return configUrl;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public boolean isRunOnlyAtRoot() {
		return runOnlyAtRoot;
	}

	public boolean isSkip() {
		return skip;
	}

	/**
	 * Returns true if the current project is located at the Execution Root Directory (where mvn was launched)
	 * 
	 * @return
	 */
	// https://blog.sonatype.com/2009/05/how-to-make-a-plugin-run-once-during-a-build/
	protected boolean isThisTheExecutionRoot() {
		LOGGER.debug("Root Folder: {}", session.getExecutionRootDirectory());

		File baseDir = getProject().getBasedir();
		LOGGER.debug("Current Folder: {}", baseDir);
		boolean result = session.getExecutionRootDirectory().equalsIgnoreCase(baseDir.toString());
		if (result) {
			LOGGER.debug("This is the execution root.");
			if (!getProject().isExecutionRoot()) {
				LOGGER.warn("Unclear if this is the executionRoot: {} vs {}", baseDir, getProject());
			}
		} else {
			LOGGER.debug("This is NOT the execution root.");
			if (getProject().isExecutionRoot()) {
				LOGGER.warn("Unclear if this is the executionRoot: {} vs {}", baseDir, getProject());
			}
		}
		return result;
	}

}