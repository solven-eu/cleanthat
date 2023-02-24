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
package eu.solven.cleanthat.spotless.language;

import java.util.List;
import java.util.Set;

import com.diffplug.spotless.scala.ScalaFmtStep;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;

/**
 * Configure Spotless engine for '.scala' files
 * 
 * @author Benoit Lacelle
 *
 */
public class ScalaFormatterFactory extends AFormatterFactory {
	// com.diffplug.spotless.maven.scala.Scala#DEFAULT_INCLUDES
	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.<String>builder()
			.add("**/src/main/scala/**/*.scala")
			.add("**/src/test/scala/**/*.scala")
			.add("**/src/main/scala/**/*.sc")
			.add("**/src/test/scala/**/*.sc")
			.build();

	@Override
	public Set<String> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	@Override
	public ScalaFormatterStepFactory makeStepFactory(ICodeProvider codeProvider,
			SpotlessFormatterProperties formatterProperties) {
		return new ScalaFormatterStepFactory(this, codeProvider, formatterProperties);
	}

	@Override
	public List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties scalafmt = SpotlessStepProperties.builder().id("scalafmt").build();
		SpotlessStepParametersProperties scalafmtParameters = new SpotlessStepParametersProperties();
		scalafmtParameters.putProperty("version", ScalaFmtStep.defaultVersion());
		scalafmtParameters.putProperty("scalaMajorVersion", ScalaFmtStep.defaultScalaMajorVersion());
		// https://scalameta.org/scalafmt/docs/configuration.html
		scalafmtParameters.putProperty(KEY_FILE, "repository:/.cleanthat/.scalafmt.conf");
		scalafmt.setParameters(scalafmtParameters);

		return ImmutableList.<SpotlessStepProperties>builder().add(scalafmt).build();
	}
}
