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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.pom.SortPomCfg;
import com.diffplug.spotless.pom.SortPomStep;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.AFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;

/**
 * Configure Spotless engine for 'pom.xml' files
 * 
 * @author Benoit Lacelle
 *
 */
public class PomXmlFormatterStepFactory extends AFormatterStepFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(PomXmlFormatterStepFactory.class);

	public PomXmlFormatterStepFactory(PomXmlFormatterFactory pomXmlFactory,
			ICodeProvider codeProvider,
			SpotlessFormatterProperties spotlessProperties) {
		super(pomXmlFactory, codeProvider, spotlessProperties);
	}

	@Override
	public String licenseHeaderDelimiter() {
		return null;
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public FormatterStep makeSpecializedStep(SpotlessStepProperties s, Provisioner provisioner) {
		String stepName = s.getId();
		SpotlessStepParametersProperties parameters = s.getParameters();

		switch (stepName) {
		case "sortPom": {
			var config = new SortPomCfg();

			parameters.getCustomProperties().forEach((customKey, customValue) -> {
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
}
