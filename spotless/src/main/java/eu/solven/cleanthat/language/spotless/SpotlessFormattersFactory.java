/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.diffplug.spotless.Provisioner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;

import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ICleanthatConfigConstants;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.engine.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.engine.IEngineStep;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.cleanthat.spotless.AFormatterFactory;
import eu.solven.cleanthat.spotless.EnrichedFormatter;
import eu.solven.cleanthat.spotless.FormatterFactory;
import eu.solven.cleanthat.spotless.SpotlessSession;
import eu.solven.cleanthat.spotless.pojo.SpotlessEngineProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessEngineProperties.SpotlessEnginePropertiesBuilder;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Formatter for Spotless Engine
 *
 * @author Benoit Lacelle
 */
@Slf4j
public class SpotlessFormattersFactory extends ASourceCodeFormatterFactory {

	final Provisioner provisionner;

	public SpotlessFormattersFactory(ConfigHelpers configHelpers, Provisioner provisionner) {
		super(configHelpers);

		this.provisionner = provisionner;
	}

	@Override
	public String getEngine() {
		return CleanthatSpotlessStepParametersProperties.ENGINE_ID;
	}

	@Override
	public Set<String> getDefaultIncludes() {
		return FormatterFactory.getDefaultIncludes();
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public ILintFixer makeLintFixer(CleanthatSession cleanthatSession,
			IEngineProperties engineProperties,
			CleanthatStepProperties stepProperties) {

		SpotlessSession spotlessSession = new SpotlessSession();

		ILintFixerWithId processor;
		var stepId = stepProperties.getId();
		// override with explicit configuration
		var parameters = getParameters(stepProperties);

		LOGGER.debug("Processing: {}", stepId);

		switch (stepId) {
		case CleanthatSpotlessStepParametersProperties.ENGINE_ID: {
			CleanthatSpotlessStepParametersProperties processorConfig =
					convertValue(parameters, CleanthatSpotlessStepParametersProperties.class);

			var spotlessConfig = processorConfig.getConfiguration();
			if (Strings.isNullOrEmpty(spotlessConfig)) {
				throw new IllegalArgumentException("'configuration' is mandatory");
			}

			var spotlessPropertiesResource =
					CleanthatUrlLoader.loadUrl(cleanthatSession.getCodeProvider(), spotlessConfig);

			SpotlessEngineProperties spotlessEngine;
			try {
				spotlessEngine = getConfigHelpers().getObjectMapper()
						.readValue(spotlessPropertiesResource.getInputStream(), SpotlessEngineProperties.class);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue loading " + spotlessConfig, e);
			}

			FormatterFactory formatterFactory = new FormatterFactory(cleanthatSession);

			List<EnrichedFormatter> formatters = spotlessEngine.getFormatters()
					.stream()
					.map(formatter -> formatterFactory.makeFormatter(spotlessEngine, formatter, provisionner))
					.collect(Collectors.toList());

			processor = new SpotlessLintFixer(spotlessSession, formatters);
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

	public static Provisioner makeProvisioner() {
		try {
			return FormatterFactory.makeProvisioner();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public CleanthatEngineProperties makeDefaultProperties(Set<String> steps) {
		var engineBuilder = CleanthatEngineProperties.builder().engine(getEngine());

		engineBuilder.step(CleanthatStepProperties.builder()
				.id(CleanthatSpotlessStepParametersProperties.STEP_ID)
				.parameters(CleanthatSpotlessStepParametersProperties.builder().build())
				.build());

		return engineBuilder.build();
	}

	@SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
	@Override
	public Map<String, String> makeCustomDefaultFiles(CleanthatEngineProperties engineProperties,
			Set<String> subStepIds) {
		Map<String, String> pathToContent = new LinkedHashMap<>();

		if (!engineProperties.getSteps().isEmpty()) {
			var singleSpotlessStep = engineProperties.getSteps().get(0);

			var configuration = (String) singleSpotlessStep.getParameters()
					.getCustomProperty(CleanthatSpotlessStepParametersProperties.KEY_CONFIGURATION);

			if (configuration != null && configuration.startsWith(CleanthatUrlLoader.PREFIX_CODE)) {
				var path = configuration.substring(CleanthatUrlLoader.PREFIX_CODE.length());

				if (path.startsWith(ICleanthatConfigConstants.PATH_SEPARATOR)) {
					path = path.substring(ICleanthatConfigConstants.PATH_SEPARATOR.length());
				}

				SpotlessEngineProperties defaultSpotlessCustomConfig = makeDefaultConfig(subStepIds);
				try {
					pathToContent.put(path,
							getConfigHelpers().getObjectMapper().writeValueAsString(defaultSpotlessCustomConfig));
				} catch (JsonProcessingException e) {
					throw new UncheckedIOException(e);
				}
			}
		}

		return pathToContent;
	}

	private SpotlessEngineProperties makeDefaultConfig(Set<String> formatIds) {
		SpotlessEnginePropertiesBuilder spotlessEngine = SpotlessEngineProperties.builder();

		formatIds.stream().sorted().forEach(formatId -> {
			SpotlessFormatterProperties defaultConfig = SpotlessFormatterProperties.builder().format(formatId).build();
			AFormatterFactory formatterFactory = FormatterFactory.makeFormatterFactory(defaultConfig);

			List<SpotlessStepProperties> exampleSteps = formatterFactory.exampleSteps();

			spotlessEngine
					.formatter(SpotlessFormatterProperties.builder().format(formatId).steps(exampleSteps).build());
		});

		return spotlessEngine.build();
	}

	@Override
	public List<IEngineStep> getMainSteps() {
		return FormatterFactory.getFormatterIds()
				.stream()
				.map(formatterId -> new SpotlessEngineFormat(formatterId))
				.collect(Collectors.toList());
	}

}
