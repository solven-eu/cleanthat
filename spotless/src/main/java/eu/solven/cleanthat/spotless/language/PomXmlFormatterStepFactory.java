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

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.pom.SortPomCfg;
import com.diffplug.spotless.pom.SortPomStep;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configure Spotless engine for 'pm.xml' files
 * 
 * @author Benoit Lacelle
 *
 */
public class PomXmlFormatterStepFactory extends AFormatterStepFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(PomXmlFormatterStepFactory.class);

	public PomXmlFormatterStepFactory(ICodeProvider codeProvider, SpotlessFormatterProperties spotlessProperties) {
		super(codeProvider, spotlessProperties);
	}

	/**
	 * CleanThat will call spotless from the root directory: process any 'pom.xml' file from there
	 */
	@Override
	public Set<String> defaultIncludes() {
		return ImmutableSet.of("**/pom.xml");
	}

	@Override
	public String licenseHeaderDelimiter() {
		return null;
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public FormatterStep makeStep(SpotlessStepProperties s, Provisioner provisioner) {
		String stepName = s.getId();
		switch (stepName) {
		case "sortPom": {
			SortPomCfg config = new SortPomCfg();

			s.getCustomProperties().forEach((customKey, customValue) -> {
				Field field;
				try {
					field = SortPomCfg.class.getField(customKey);
				} catch (NoSuchFieldException | SecurityException e) {
					LOGGER.warn("Not managed customProperty: {}={}", customKey, customValue, e);
					return;
				}

				try {
					field.set(config, customValue);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					LOGGER.warn("Issue propagating customProperty: {}={}", customKey, customValue, e);
					return;
				}
			});

			return SortPomStep.create(config, provisioner);
		}
		default: {
			throw new IllegalArgumentException("Unknown Java step: " + stepName);
		}
		}
	}

	// This is useful to demonstrate all available configuration
	public static List<SpotlessStepProperties> exampleSteps() {
		SpotlessStepProperties sortPom = new SpotlessStepProperties();
		sortPom.setId("sortPom");
		SortPomCfg defaultSortPomConfig = new SortPomCfg();
		for (Field f : SortPomCfg.class.getFields()) {
			try {
				sortPom.putProperty(f.getName(), f.get(defaultSortPomConfig));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.warn("Issue fethcing default value for field={}", f, e);
			}
		}

		return ImmutableList.<SpotlessStepProperties>builder().add(sortPom).build();
	}

}
