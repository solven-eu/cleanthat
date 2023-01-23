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

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * The parameters of a {@link CleanthatStepProperties}. They are typically custom per engin/step.
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
// @Builder
// @Jacksonized
public final class CleanthatStepParametersProperties implements ICleanthatStepParametersProperties {

	// https://stackoverflow.com/questions/32235993/mix-of-standard-and-dynamic-properties-in-jackson-mapping
	// https://stackoverflow.com/questions/61165401/make-jsonanysetter-work-with-value-lombok
	// @Singular("add")
	@JsonIgnore
	private Map<String, Object> any = new LinkedHashMap<>();

	@JsonAnySetter
	public void add(String key, Object value) {
		any.put(key, value);
	}

	@JsonAnyGetter
	public Map<String, ?> getCustomProperties() {
		return any;
	}

	@Override
	public Object getCustomProperty(String key) {
		return any.get(key);
	}
}
