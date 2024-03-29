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
package eu.solven.cleanthat.language.openrewrite;

import java.util.List;

import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;

import com.google.common.collect.ImmutableList;

import io.vavr.collection.Set;

/**
 * Relates to the {@link Set} of safe and consensual {@link Recipe} available in OpenRewrite.
 * 
 * @author Benoit Lacelle
 *
 */
public class SafeAndConsensualRecipe {
	// org.openrewrite.java.security.JavaSecurityBestPractices
	private static final List<String> RECIPES = ImmutableList.<String>builder()
			// Core
			.add("org.openrewrite.java.RemoveUnusedImports")
			// SLF4J
			.add("org.openrewrite.java.logging.slf4j.ParameterizedLogging")
			.build();

	protected SafeAndConsensualRecipe() {
		// hidden
	}

	// @Override
	// public String getDisplayName() {
	// return JavaRefactorerProperties.SAFE_AND_CONSENSUAL;
	// }

	public static Recipe makeRecipe() {
		Environment environment = Environment.builder().scanRuntimeClasspath().build();
		Recipe recipe = environment.activateRecipes(RECIPES);
		return recipe;
	}
}
