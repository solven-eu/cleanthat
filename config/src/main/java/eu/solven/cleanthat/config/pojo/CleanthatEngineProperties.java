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
package eu.solven.cleanthat.config.pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.language.IEngineProperties;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

/**
 * The configuration of what is not related to a language.
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.ImmutableField")
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties({ "language", "language_version", "processors" })
// Order is defined by fields definition
// @JsonPropertyOrder(alphabetic = true, value = { "language", "language_version", ISkippable.KEY_SKIP })
public class CleanthatEngineProperties implements IEngineProperties {
	private String engine;

	// https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
	private String engineVersion;

	// By default, we do not skip
	@Builder.Default
	private boolean skip = false;

	private SourceCodeProperties sourceCode;

	// The (ordered) steps to apply
	@Singular
	private List<CleanthatStepProperties> steps;

}
