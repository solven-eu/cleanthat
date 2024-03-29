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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.springframework.context.ApplicationContext;

import eu.solven.cleanthat.any_language.ICodeCleaner;
import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.lambda.AllEnginesSpringConfig;

/**
 * The mojo doing actual cleaning
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
@Mojo(name = CleanThatCleanThatMojo.MOJO_FIX,
		defaultPhase = LifecyclePhase.PROCESS_SOURCES,
		threadSafe = true,
		// Used to enable symbolSolving based on project dependencies
		requiresDependencyResolution = ResolutionScope.RUNTIME,
		// One may rely on the mvn plugin to clean a folder, even if no pom.xml is available
		requiresProject = false)
public class CleanThatCleanThatMojo extends ACleanThatSpringMojo {
	public static final String MOJO_FIX = "cleanthat";

	static List<Class<?>> cleanThatSpringClasses() {
		List<Class<?>> classes = new ArrayList<>();

		classes.add(GithubSpringConfig.class);
		classes.add(AllEnginesSpringConfig.class);
		classes.add(CodeProviderHelpers.class);

		return classes;
	}

	@Override
	protected List<Class<?>> springClasses() {
		return cleanThatSpringClasses();
	}

	@Override
	public void doClean(ApplicationContext appContext) {
		if (isRunOnlyAtRoot() && !isThisTheExecutionRoot()) {
			// This will check it is called only if the command is run from the project root.
			// However, it will not prevent the plugin to be called on each module
			getLog().info("maven-cleanthat-plugin:" + MOJO_FIX + " skipped (not execution root)");
			return;
		}

		ICodeProviderWriter codeProvider = CleanThatMavenHelper.makeCodeProviderWriter(this);
		ICodeCleaner codeCleaner = CleanThatMavenHelper.makeCodeCleaner(appContext);
		codeCleaner.formatCodeGivenConfig(CleanThatCleanThatMojo.class.getSimpleName(), codeProvider, isDryRun());
	}
}
