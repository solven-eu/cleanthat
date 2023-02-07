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

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

/**
 * Used to configure a specific Spotless format
 * 
 * @author Benoit Lacelle
 *
 */
// https://github.com/diffplug/spotless/tree/main/plugin-gradle#quickstart
// https://github.com/diffplug/spotless/tree/main/plugin-maven#quickstart
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
@Builder
@Jacksonized
public class SpotlessFormatterProperties {
	// If left to default, we will rely on the engine encoding
	private String encoding;

	// java, json, etc (or a generic 'format')
	private String format;
	// Any String. Useful when multiple formatters rely on the same format
	private String alias;

	private List<String> includes;
	private List<String> excludes;

	// The steps of given language
	@Singular
	private List<SpotlessStepProperties> steps;
}
