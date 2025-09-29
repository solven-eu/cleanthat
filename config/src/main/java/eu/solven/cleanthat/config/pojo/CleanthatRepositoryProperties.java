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
package eu.solven.cleanthat.config.pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.github.IHasSourceCodeProperties;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

/**
 * The configuration of a formatting job
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
// https://stackoverflow.com/questions/19272830/order-of-json-objects-using-jacksons-objectmapper
@JsonPropertyOrder({ "syntax_version", "meta", "source_code", "engines" })
@JsonIgnoreProperties({ "languages" })
@Data
@Builder
@Jacksonized
public final class CleanthatRepositoryProperties implements IHasSourceCodeProperties {
	public static final String PREVIOUS_SYNTAX_VERSION = "2021-08-02";
	public static final String LATEST_SYNTAX_VERSION = "2023-01-09";

	// Not named 'config_version' else it may be unclear if it applies to that config_syntax or to the user_config
	// version
	// AWS IAM policy relies on a field named 'Version' with a localDate as value: it is a source of inspiration
	@Builder.Default
	private String syntaxVersion = LATEST_SYNTAX_VERSION;

	@Builder.Default
	private CleanthatMetaProperties meta = CleanthatMetaProperties.builder().build();

	// Properties to apply to each children
	@Builder.Default
	private SourceCodeProperties sourceCode = SourceCodeProperties.defaultChild();

	// @JsonProperty(index = -999)
	@Singular
	private List<CleanthatEngineProperties> engines;

	public static CleanthatRepositoryProperties defaultRepository() {
		CleanthatRepositoryProperties root = CleanthatRepositoryProperties.builder().build();

		root.setSourceCode(SourceCodeProperties.defaultRoot());

		return root;
	}

}
