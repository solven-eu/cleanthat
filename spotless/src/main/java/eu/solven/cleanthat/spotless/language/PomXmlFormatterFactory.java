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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.pom.SortPomCfg;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
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
public class PomXmlFormatterFactory extends AFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(PomXmlFormatterFactory.class);

	/**
	 * CleanThat will call spotless from the root directory: process any 'pom.xml' file from there
	 */
	@Override
	public Set<String> defaultIncludes() {
		return ImmutableSet.of("**/pom.xml");
	}

	@Override
	public PomXmlFormatterStepFactory makeStepFactory(ICodeProvider codeProvider,
			SpotlessFormatterProperties formatterProperties) {
		return new PomXmlFormatterStepFactory(this, codeProvider, formatterProperties);
	}

	@Override
	public List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties sortPom = SpotlessStepProperties.builder().id("sortPom").build();
		SpotlessStepParametersProperties sortPomParameters = new SpotlessStepParametersProperties();
		var defaultSortPomConfig = new SortPomCfg();
		for (Field f : SortPomCfg.class.getFields()) {
			try {
				sortPomParameters.putProperty(f.getName(), f.get(defaultSortPomConfig));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.warn("Issue fethcing default value for field={}", f, e);
			}
		}
		sortPom.setParameters(sortPomParameters);

		return ImmutableList.<SpotlessStepProperties>builder().add(sortPom).build();
	}

}
