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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import eu.solven.cleanthat.language.IEngineProperties;
import java.util.Arrays;
import java.util.List;
import lombok.Data;

/**
 * The configuration of what is not related to a language.
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.ImmutableField")
@Data
@JsonIgnoreProperties({ "language", "language_version" })
// Order is defined by fields definition
// @JsonPropertyOrder(alphabetic = true, value = { "language", "language_version", ISkippable.KEY_SKIP })
public class CleanthatEngineProperties implements IEngineProperties {
	public static final String NO_ENGINE = "none";

	private String engine = NO_ENGINE;

	// https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
	private String engineVersion = "0";

	// By default, we do not skip
	private boolean skip = false;

	private SourceCodeProperties sourceCode = new SourceCodeProperties();

	// The (ordered) steps to apply
	// @JsonDeserialize(using = ProcessorsDeseralizer.class)
	private List<CleanthatStepProperties> steps = Arrays.asList();

}
