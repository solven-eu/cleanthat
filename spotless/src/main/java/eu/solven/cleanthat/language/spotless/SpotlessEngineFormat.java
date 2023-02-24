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
package eu.solven.cleanthat.language.spotless;

import java.util.Set;
import java.util.stream.Collectors;

import eu.solven.cleanthat.engine.IEngineStep;
import eu.solven.cleanthat.spotless.FormatterFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;

/**
 * Represent each available formatter from Spotless engine
 * 
 * @author Benoit Lacelle
 */
public class SpotlessEngineFormat implements IEngineStep {
	public static final String ID_SPOTLESS = ICleanthatSpotlessConstants.STEP_ID;

	final String format;

	public SpotlessEngineFormat(String format) {
		this.format = format;
	}

	@Override
	public String getStep() {
		return format;
	}

	@Override
	public Set<String> getDefaultIncludes() {
		SpotlessFormatterProperties defaultConfig = SpotlessFormatterProperties.builder().format(format).build();
		return FormatterFactory.makeFormatterFactory(defaultConfig)
				.defaultIncludes()
				.stream()
				.map(s -> FormatterFactory.prefixWithGlob(s))
				.collect(Collectors.toSet());
	}
}
