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
package eu.solven.cleanthat.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatMetaProperties;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

public class TestCleanthatMetaProperties {
	final ObjectMapper objectMapper = ConfigHelpers.makeJsonObjectMapper();

	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(CleanthatMetaProperties.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testDefaultConstructor() throws JsonProcessingException {
		CleanthatMetaProperties p = new CleanthatMetaProperties();

		String json = objectMapper.writeValueAsString(p);

		CleanthatMetaProperties backToObject = objectMapper.readValue(json, CleanthatMetaProperties.class);

		Assert.assertEquals(p, backToObject);
		Assertions.assertThat(backToObject.getLabels()).containsExactly("cleanthat");
		Assertions.assertThat(backToObject.getRefs()).isNotNull();
	}
}
