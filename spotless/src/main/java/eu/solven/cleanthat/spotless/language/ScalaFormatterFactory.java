/*
 * Copyright 2023 Solven
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

import com.google.common.collect.ImmutableSet;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import java.util.Set;

/**
 * Configure Spotless engine for '.scala' files
 * 
 * @author Benoit Lacelle
 *
 */
public class ScalaFormatterFactory extends AFormatterFactory {
	// com.diffplug.spotless.maven.scala.Scala#DEFAULT_INCLUDES
	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.of("src/main/scala/**/*.scala",
			"src/test/scala/**/*.scala",
			"src/main/scala/**/*.sc",
			"src/test/scala/**/*.sc");

	@Override
	public Set<String> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	@Override
	public ScalaFormatterStepFactory makeStepFactory(ICodeProvider codeProvider,
			SpotlessFormatterProperties formatterProperties) {
		return new ScalaFormatterStepFactory(this, codeProvider, formatterProperties);
	}
}
