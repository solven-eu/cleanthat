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
package eu.solven.cleanthat.engine;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatStepParametersProperties;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class TestCleanthatStepParametersProperties {
	final ObjectMapper objectMapper = ConfigHelpers.makeJsonObjectMapper();

	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(CleanthatStepParametersProperties.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testDefaultConstructor() throws JsonProcessingException {
		var p = new CleanthatStepParametersProperties();

		p.add("k1", "v1");

		var json = objectMapper.writeValueAsString(p);

		var backToObject = objectMapper.readValue(json, CleanthatStepParametersProperties.class);

		Assert.assertEquals(p, backToObject);
		Assertions.assertThat(backToObject.getCustomProperty("k1")).isEqualTo("v1");
	}

	@Test
	public void testCustomConfig() throws JsonProcessingException {
		CleanthatCustomStepParametersProperties p =
				CleanthatCustomStepParametersProperties.builder().someKey("someValue").build();

		CleanthatStepParametersProperties genericConfig =
				objectMapper.convertValue(p, CleanthatStepParametersProperties.class);

		var json = objectMapper.writeValueAsString(genericConfig);

		var backToGeneric = objectMapper.readValue(json, CleanthatStepParametersProperties.class);

		CleanthatCustomStepParametersProperties backToCustomThroughGeneric =
				objectMapper.convertValue(backToGeneric, CleanthatCustomStepParametersProperties.class);

		CleanthatCustomStepParametersProperties backToCustom =
				objectMapper.readValue(json, CleanthatCustomStepParametersProperties.class);

		Assert.assertEquals(p, backToCustomThroughGeneric);
		Assert.assertEquals(p, backToCustom);

	}
}
