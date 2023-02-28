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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import eu.solven.cleanthat.config.ICleanthatConfigConstants;

/**
 * The mojo of the mvn plugin
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
public abstract class ACleanThatMojo extends AbstractMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatInitMojo.class);

	// To be synced with CodeProviderHelpers.PATHES_CLEANTHAT.get(0)
	public static final String MARKER_ANY_PARENT_DOTCLEANTHAT =
			"glob:**/" + ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT;

	// http://maven.apache.org/ref/3.1.1/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	private MavenSession session;

	// @Parameter(defaultValue = "${session.executionRootDirectory}", required = true, readonly = true)
	// private String executionRootDirectory;

	// https://stackoverflow.com/questions/3084629/finding-the-root-directory-of-a-multi-module-maven-reactor-project
	// It seems not possible to rely on 'maven.XXX' (hence not '${maven.multiModuleProjectDirectory}') in plugin
	// parameters. See PluginParameterExpressionEvaluator
	// @Parameter(property = "cleanthat.configPath", defaultValue = "${session.basedir}/cleanthat.yaml")
	// @Parameter(property = "cleanthat.configPath", defaultValue = "${project.basedir}/cleanthat.yaml")
	// The following is not compatible with goals executable without a pom.xml
	// @Parameter(property = "cleanthat.configPath", defaultValue = "${session.topLevelProject.basedir}/cleanthat.yaml")
	// A typical value is '-Dcleanthat.configPath=${session.executionRootDirectory}/.cleanthat/cleanthat.yaml'
	@Parameter(property = "cleanthat.configPath", defaultValue = MARKER_ANY_PARENT_DOTCLEANTHAT)
	private String cleanthatRepositoryConfigPath;

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
	 * 
	 * Cleanthat will process current directory: it does not need to iterate over modules which are children in the
	 * {@link FileSystem}
	 */
	// https://github.com/maven-download-plugin/maven-download-plugin/blob/master/src/main/java/com/googlecode/download/maven/plugin/internal/WGet.java
	// https://github.com/khmarbaise/maven-assembly-plugin/blob/master/src/main/java/org/apache/maven/plugin/assembly/mojos/AbstractAssemblyMojo.java#L335
	@Parameter(property = "runOnlyAtRoot", defaultValue = "true")
	private boolean runOnlyAtRoot;

	FileSystem fs = FileSystems.getDefault();

	@Deprecated(since = "Only for testing")
	public void setFileSystem(FileSystem fs) {
		this.fs = fs;
	}

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

	@VisibleForTesting
	public void setCleanthatRepositoryConfigPath(String cleanthatRepositoryConfigPath) {
		this.cleanthatRepositoryConfigPath = cleanthatRepositoryConfigPath;
	}

	protected void checkParameters() {
		var configPath = getRepositoryConfigPath();

		if (!Files.exists(configPath)) {
			throw new IllegalArgumentException("There is no configuration at: " + configPath);
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

	public Path getRepositoryConfigPath() {
		if (Strings.isNullOrEmpty(cleanthatRepositoryConfigPath)) {
			throw new IllegalArgumentException("'cleanthatRepositoryConfigPath' must not be null");
		} else if (MARKER_ANY_PARENT_DOTCLEANTHAT.equals(cleanthatRepositoryConfigPath)) {
			// We apply the default strategy: iterate through parentFolder for '.cleanthat/cleanthat.yaml'
			String rawExecutionRootDirectory = session.getExecutionRootDirectory();
			var executionRootDirectory = fs.getPath(rawExecutionRootDirectory);

			if (!Files.isDirectory(executionRootDirectory)) {
				throw new IllegalStateException("Not a directory: " + executionRootDirectory);
			}

			var rootOrAncestor = executionRootDirectory;
			do {
				var configPath = rootOrAncestor.resolve(ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT);
				if (Files.isRegularFile(configPath)) {
					LOGGER.debug("Main configurationFile: {}", configPath);
					return configPath;
				} else {
					LOGGER.debug("{} does not exist", configPath);
					rootOrAncestor = rootOrAncestor.getParent();
				}
			} while (rootOrAncestor.getParent() != null);

			throw new IllegalArgumentException(
					"We do not find a relative " + ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT
							+ " from current of ancestors directories");
		} else {
			if (cleanthatRepositoryConfigPath.contains("${")) {
				throw new IllegalStateException(
						"Some placeholders are left in cleanthatRepositoryConfigPath: '" + cleanthatRepositoryConfigPath
								+ "'");
			}

			// We have been requested for a specific configuration path
			var manuaPathToConfig = fs.getPath(cleanthatRepositoryConfigPath).toAbsolutePath();

			if (!Files.exists(manuaPathToConfig)) {
				throw new IllegalArgumentException("There is no configuration at: " + manuaPathToConfig);
			}

			return manuaPathToConfig;
		}
	}

	public Path getMayNotExistRepositoryConfigPath() {
		if (Strings.isNullOrEmpty(cleanthatRepositoryConfigPath)) {
			throw new IllegalArgumentException("'cleanthatRepositoryConfigPath' must not be null");
		} else if (MARKER_ANY_PARENT_DOTCLEANTHAT.equals(cleanthatRepositoryConfigPath)) {
			// We apply the default strategy: iterate through parentFolder for '.cleanthat/cleanthat.yaml'
			String rawExecutionRootDirectory = session.getExecutionRootDirectory();
			var executionRootDirectory = fs.getPath(rawExecutionRootDirectory);

			if (!Files.isDirectory(executionRootDirectory)) {
				throw new IllegalStateException("Not a directory: " + executionRootDirectory);
			}

			var rootOrAncestor = executionRootDirectory;
			var configPath = rootOrAncestor.resolve(ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT);
			LOGGER.debug("Main configurationFile: {}", configPath);
			return configPath;
		} else {
			if (cleanthatRepositoryConfigPath.contains("${")) {
				throw new IllegalStateException(
						"Some placeholders are left in cleanthatRepositoryConfigPath: '" + cleanthatRepositoryConfigPath
								+ "'");
			}

			// We have been requested for a specific configuration path
			var manuaPathToConfig = fs.getPath(cleanthatRepositoryConfigPath).toAbsolutePath();

			return manuaPathToConfig;
		}
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
		String executionRootDirectory = session.getExecutionRootDirectory();
		LOGGER.debug("getExecutionRootDirectory(): {}", executionRootDirectory);

		File baseDir = getProject().getBasedir().getAbsoluteFile();
		LOGGER.debug("getProject().getBasedir().getAbsoluteFile(): {}", baseDir);
		var result = executionRootDirectory.equalsIgnoreCase(baseDir.toString());

		File projectFile = getProject().getBasedir();
		sanityChecks(baseDir, result, projectFile);

		return result;
	}

	@SuppressWarnings("PMD.InvalidLogMessageFormat")
	private void sanityChecks(File baseDir, boolean result, File projectFile) {
		String template =
				": Unclear what is the executionRoot: getExecutionRootDirectory()={} vs getProject().getBasedir()={}";
		if (result) {
			LOGGER.debug("This is the execution root.");
			if (!getProject().isExecutionRoot()) {
				LOGGER.warn("`getProject().isExecutionRoot()==false`" + template, baseDir, projectFile);
			}
		} else {
			LOGGER.debug("This is NOT the execution root.");
			if (getProject().isExecutionRoot()) {
				LOGGER.warn("`getProject().isExecutionRoot()==true`" + template, baseDir, projectFile);
			}
		}
	}

}