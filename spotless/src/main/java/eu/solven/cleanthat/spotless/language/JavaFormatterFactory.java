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

import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.spotless.AFormatterFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;

/**
 * Configure Spotless engine for '.java' files
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaFormatterFactory extends AFormatterFactory {
	// CleanThat will call spotless from the root directory: process any Java file from there, in some 'src' parent
	// directory
	private static final Set<String> DEFAULT_INCLUDES = ImmutableSet.of("**/src/**/*.java");

	@Override
	public Set<String> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	@Override
	public JavaFormatterStepFactory makeStepFactory(ICodeProvider codeProvider,
			SpotlessFormatterProperties formatterProperties) {
		return new JavaFormatterStepFactory(this, codeProvider, formatterProperties);
	}

	// This is useful to demonstrate all available configuration
	@Override
	public List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties removeUnusedImports = SpotlessStepProperties.builder().id("removeUnusedImports").build();

		SpotlessStepProperties importOrder = SpotlessStepProperties.builder()
				.id("importOrder")
				// Skipped as it would require saving additional custom files
				// Skipped as it may change too much code
				.skip(true)
				.build();
		SpotlessStepParametersProperties importOrderParameters = new SpotlessStepParametersProperties();
		importOrderParameters.putProperty(JavaFormatterStepFactory.KEY_FILE,
				CleanthatUrlLoader.PREFIX_CODE + ".cleanthat/java-importorder.properties");
		// importOrderParameters.putProperty(JavaFormatterStepFactory.KEY_ORDER,
		// JavaFormatterStepFactory.ORDER_DEFAULT_ECLIPSE);
		importOrder.setParameters(importOrderParameters);

		// Cleanthat before Eclipse as CleanThat may break the style
		SpotlessStepProperties cleanthat = SpotlessStepProperties.builder().id("cleanthat").build();
		SpotlessStepParametersProperties cleanthatParameters = new SpotlessStepParametersProperties();
		cleanthatParameters.putProperty("source_jdk", "11");
		cleanthatParameters.putProperty("mutators", JavaFormatterStepFactory.DEFAULT_MUTATORS);
		cleanthat.setParameters(cleanthatParameters);

		SpotlessStepProperties eclipse = SpotlessStepProperties.builder()
				.id(JavaFormatterStepFactory.ID_ECLIPSE)
				// Skipped as it would require saving additional custom files
				// Skipped as it may change too much code
				.skip(true)
				.build();
		SpotlessStepParametersProperties eclipseParameters = new SpotlessStepParametersProperties();
		eclipseParameters.putProperty("version", EclipseJdtFormatterStep.defaultVersion());
		eclipseParameters.putProperty(JavaFormatterStepFactory.KEY_FILE,
				CleanthatUrlLoader.PREFIX_CODE + JavaFormatterStepFactory.DEFAULT_ECLIPSE_FILE);
		eclipse.setParameters(eclipseParameters);

		return ImmutableList.<SpotlessStepProperties>builder()
				// CleanThat is first as it may generate unoptimized imports and break stylesheet
				.add(cleanthat)
				.add(removeUnusedImports)
				.add(importOrder)
				.add(eclipse)
				.build();
	}

	public static SpotlessStepProperties makeDefaultEclipseStep() {
		SpotlessStepProperties eclipse =
				SpotlessStepProperties.builder().id(JavaFormatterStepFactory.ID_ECLIPSE).build();
		SpotlessStepParametersProperties eclipseParameters = new SpotlessStepParametersProperties();
		eclipseParameters.putProperty(JavaFormatterStepFactory.KEY_ECLIPSE_FILE,
				CleanthatUrlLoader.PREFIX_CODE + JavaFormatterStepFactory.DEFAULT_ECLIPSE_FILE);
		eclipse.setParameters(eclipseParameters);

		return eclipse;
	}
}
