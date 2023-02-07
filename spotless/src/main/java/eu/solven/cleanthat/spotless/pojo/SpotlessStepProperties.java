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
package eu.solven.cleanthat.spotless.pojo;

import com.diffplug.spotless.FormatterStep;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Helps configuring a {@link FormatterStep}. Any dynamic property is accepted, and available at runtime through
 * .getCustomProperty
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
@Builder
@Jacksonized
// @JsonDeserialize(converter = SpotlessStepPropertiesSanitizer.class)
public class SpotlessStepProperties {

	// the step name/id
	@lombok.NonNull
	private final String id;

	// the step parameters
	@Builder.Default
	private SpotlessStepParametersProperties parameters = new SpotlessStepParametersProperties();
}
