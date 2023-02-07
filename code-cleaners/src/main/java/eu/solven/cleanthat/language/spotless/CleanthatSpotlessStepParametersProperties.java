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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.config.pojo.ICleanthatStepParametersProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * The configuration of Spotless step.
 * 
 * BEWARE This is in 'code-cleaners' module as it is part of the default cleanthat configuration (e.g. when bootstraping
 * a new repository)
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressWarnings("PMD.ImmutableField")
@Data
@Builder
@Jacksonized
public class CleanthatSpotlessStepParametersProperties
		implements ICleanthatStepParametersProperties, ICleanthatSpotlessConstants {
	public static final String KEY_CONFIGURATION = "configuration";

	// The default configuration location is the first option amongst the possible locations
	public static final String DEFAULT_CONFIGURATION =
			CodeProviderHelpers.PATH_SEPARATOR + CodeProviderHelpers.FILENAME_CLEANTHAT_FOLDER
					+ CodeProviderHelpers.PATH_SEPARATOR
					+ "spotless.yaml";

	@Builder.Default
	private String configuration = CleanthatUrlLoader.PREFIX_CODE + DEFAULT_CONFIGURATION;

	@Override
	public Object getCustomProperty(String key) {
		if (KEY_CONFIGURATION.equalsIgnoreCase(key)) {
			return configuration;
		}
		return null;
	}
}
