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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.springframework.context.ApplicationContext;

import eu.solven.cleanthat.any_language.ICodeCleaner;
import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.lambda.AllEnginesSpringConfig;

/**
 * The mojo checking the code is clean
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
// Should the default phase be VALIDATE (i.e. the very first) or VERIFY (i.e. the very last)?
// Checkstyle is VERIFY: https://maven.apache.org/plugins/maven-checkstyle-plugin/check-mojo.html
// PMD is VERIFY: https://maven.apache.org/plugins/maven-pmd-plugin/check-mojo.html
// SpotBugs is VERIFY: https://spotbugs.github.io/spotbugs-maven-plugin/check-mojo.html
// Revelc is VALIDATE: https://code.revelc.net/formatter-maven-plugin/validate-mojo.html
@Mojo(name = CleanThatCheckMojo.MOJO_CHECK, defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CleanThatCheckMojo extends ACleanThatSpringMojo {
	public static final String MOJO_CHECK = "check";

	@Override
	protected List<Class<?>> springClasses() {
		List<Class<?>> classes = new ArrayList<>();

		classes.add(GithubSpringConfig.class);
		classes.add(AllEnginesSpringConfig.class);
		classes.add(CodeProviderHelpers.class);

		return classes;
	}

	// TODO Inspire from https://maven.apache.org/plugins/maven-pmd-plugin/check-mojo.html
	@Override
	protected void doClean(ApplicationContext appContext) throws IOException, MojoFailureException {
		checkParameters();

		getLog().info("Path: " + getConfigPath());
		getLog().info("URL: " + getConfigUrl());

		ICodeProviderWriter codeProvider = CleanThatMavenHelper.makeCodeProviderWriter(this);
		ICodeCleaner codeCleaner = CleanThatMavenHelper.makeCodeCleaner(appContext);
		CodeFormatResult result = codeCleaner.formatCodeGivenConfig(codeProvider, true);

		if (!result.isEmpty()) {
			throw new MojoFailureException("ARG",
					"CleanThat would have impacted the code",
					"CleanThat would have impacted the code");
		}
	}
}