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
package eu.solven.cleanthat.language.spotless;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.Provisioner;
import com.google.common.base.Strings;

import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.config.pojo.ICleanthatStepParametersProperties;
import eu.solven.cleanthat.engine.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.cleanthat.spotless.FormatterFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessEngineProperties;

/**
 * Formatter for Spotless Engine
 *
 * @author Benoit Lacelle
 */
public class SpotlessFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpotlessFormattersFactory.class);

	public SpotlessFormattersFactory(ConfigHelpers configHelpers) {
		super(configHelpers);
	}

	@Override
	public String getEngine() {
		return CleanthatSpotlessStepParametersProperties.ENGINE_ID;
	}

	@Override
	public Set<String> getFileExtentions() {
		return Set.of("java", "scala", "json");
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public ILintFixer makeLintFixer(CleanthatStepProperties stepProperties,
			IEngineProperties languageProperties,
			CleanthatSession cleanthatSession) {
		ILintFixerWithId processor;
		String stepId = stepProperties.getId();
		// override with explicit configuration
		ICleanthatStepParametersProperties parameters = getParameters(stepProperties);

		LOGGER.debug("Processing: {}", stepId);

		switch (stepId) {
		case CleanthatSpotlessStepParametersProperties.ENGINE_ID: {
			CleanthatSpotlessStepParametersProperties processorConfig =
					convertValue(parameters, CleanthatSpotlessStepParametersProperties.class);

			String spotlessConfig = processorConfig.getConfiguration();
			if (Strings.isNullOrEmpty(spotlessConfig)) {
				throw new IllegalArgumentException("'configuration' is mandatory");
			}

			Resource spotlessPropertiesResource =
					CleanthatUrlLoader.loadUrl(cleanthatSession.getCodeProvider(), spotlessConfig);

			SpotlessEngineProperties spotlessEngine;
			try {
				spotlessEngine = getConfigHelpers().getObjectMapper()
						.readValue(spotlessPropertiesResource.getInputStream(), SpotlessEngineProperties.class);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue loading " + spotlessConfig, e);
			}

			FormatterFactory formatterFactory = new FormatterFactory(cleanthatSession);

			List<Formatter> formatters = spotlessEngine.getFormatters()
					.stream()
					.map(formatter -> formatterFactory.makeFormatter(spotlessEngine, formatter, makeProvisionner()))
					.collect(Collectors.toList());

			processor = new SpotlessLintFixer(formatters);
			break;
		}

		default:
			throw new IllegalArgumentException("Unknown step: " + stepId);
		}

		if (!processor.getId().equals(stepId)) {
			throw new IllegalStateException("Inconsistency: " + processor.getId() + " vs " + stepId);
		}

		return processor;
	}

	protected Provisioner makeProvisionner() {
		return FormatterFactory.makeProvisionner();
	}

	@Override
	public CleanthatEngineProperties makeDefaultProperties() {
		return CleanthatEngineProperties.builder()
				.engine(getEngine())
				.step(CleanthatStepProperties.builder()
						.id(CleanthatSpotlessStepParametersProperties.STEP_ID)
						.parameters(CleanthatSpotlessStepParametersProperties.builder().build())
						.build())
				.build();
	}

}
