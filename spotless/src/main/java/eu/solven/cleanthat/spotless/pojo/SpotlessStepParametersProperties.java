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
package eu.solven.cleanthat.spotless.pojo;

import com.diffplug.spotless.FormatterStep;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

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
public class SpotlessStepParametersProperties {

	// https://stackoverflow.com/questions/32235993/mix-of-standard-and-dynamic-properties-in-jackson-mapping
	@JsonIgnore
	private Map<String, Object> customProperties = new LinkedHashMap<>();

	@JsonAnySetter
	public void putProperty(String key, Object value) {
		customProperties.put(key, value);
	}

	@JsonAnyGetter
	public Map<String, Object> getCustomProperties() {
		return customProperties;
	}

	public <T> T getCustomProperty(String key, Class<? extends T> clazz) {
		return clazz.cast(customProperties.get(key));
	}
}
