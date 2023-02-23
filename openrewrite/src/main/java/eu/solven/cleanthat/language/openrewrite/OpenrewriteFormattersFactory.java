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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepParametersProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.config.pojo.ICleanthatStepParametersProperties;
import eu.solven.cleanthat.engine.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.IEngineProperties;

/**
 * Formatter for OpenRewrite Engine
 *
 * @author Benoit Lacelle
 */
public class OpenrewriteFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenrewriteFormattersFactory.class);

	public OpenrewriteFormattersFactory(ConfigHelpers configHelpers) {
		super(configHelpers);
	}

	@Override
	public String getEngine() {
		return "openrewrite";
	}

	@Override
	public Set<String> getDefaultIncludes() {
		return Set.of("glob:**/*.java");
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public ILintFixer makeLintFixer(CleanthatSession cleanthatSession,
			IEngineProperties engineProperties,
			CleanthatStepProperties stepProperties) {

		ILintFixerWithId lintFixer;
		String stepId = stepProperties.getId();
		// override with explicit configuration
		ICleanthatStepParametersProperties parameters = getParameters(stepProperties);

		LOGGER.debug("Processing: {}", stepId);

		switch (stepId) {
		case "openrewrite": {
			// "org.openrewrite.java.cleanup.CommonStaticAnalysis"
			Collection<String> rawRecipes = (Collection<String>) parameters.getCustomProperty("recipes");

			if (rawRecipes == null) {
				rawRecipes = getDefaultRecipes();
			}

			// put any rewrite recipe jars on this main method's runtime classpath
			// and either construct the recipe directly or via an Environment
			Environment environment = Environment.builder().scanRuntimeClasspath().build();
			Recipe recipe = environment.activateRecipes(rawRecipes);

			lintFixer = new OpenrewriteLintFixer(recipe);
			break;
		}

		default:
			throw new IllegalArgumentException("Unknown step: " + stepId);
		}

		if (!lintFixer.getId().equals(stepId)) {
			throw new IllegalStateException("Inconsistency: " + lintFixer.getId() + " vs " + stepId);
		}

		return lintFixer;
	}

	private List<String> getDefaultRecipes() {
		// "org.openrewrite.java.cleanup.CommonStaticAnalysis" may not be consensual
		return ImmutableList.<String>builder()
				// This is consensual
				.add("org.openrewrite.java.RemoveUnusedImports")
				// This may not be consensual, it it provides a lot of value
				.add("org.openrewrite.java.security.JavaSecurityBestPractices")
				.build();
	}

	@Override
	public CleanthatEngineProperties makeDefaultProperties() {
		CleanthatStepParametersProperties stepProperties = new CleanthatStepParametersProperties();
		stepProperties.add("recipes", getDefaultRecipes());

		return CleanthatEngineProperties.builder()
				.engine(getEngine())
				.step(CleanthatStepProperties.builder().id("openrewrite").parameters(stepProperties).build())
				.build();
	}

	@Override
	public Map<String, String> makeCustomDefaultFiles(CleanthatEngineProperties engineProperties) {
		return Collections.emptyMap();
	}

}
