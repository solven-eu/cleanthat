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
package eu.solven.cleanthat.config.pojo;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Some metadata properties.
 *
 * @author Benoit Lacelle
 */
@JsonIgnoreProperties({ "commit_pull_requests", "commit_main_branch" })
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
@Builder
@Jacksonized
public final class CleanthatMetaProperties {

	// The labels to apply to created PRs
	@Builder.Default
	private List<String> labels = Arrays.asList("cleanthat");

	@Builder.Default
	private CleanthatRefFilterProperties refs = new CleanthatRefFilterProperties();

	@Builder.Default
	private boolean fullCleanOnConfigurationChange = false;

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = List.copyOf(labels);
	}

	public CleanthatRefFilterProperties getRefs() {
		return refs;
	}

	public void setRefs(CleanthatRefFilterProperties refs) {
		this.refs = refs;
	}

}
