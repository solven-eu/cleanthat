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
import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

public class TestCleanthatRefFilterProperties {
	final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testDefaultConstructor() throws JsonProcessingException {
		CleanthatRefFilterProperties p = new CleanthatRefFilterProperties();

		String json = objectMapper.writeValueAsString(p);

		CleanthatRefFilterProperties backToObject = objectMapper.readValue(json, CleanthatRefFilterProperties.class);

		Assert.assertEquals(p, backToObject);
	}

	@Test
	public void testEmptyJson() throws JsonProcessingException {
		CleanthatRefFilterProperties p = new CleanthatRefFilterProperties();

		String json = "{}";

		CleanthatRefFilterProperties backToObject = objectMapper.readValue(json, CleanthatRefFilterProperties.class);

		Assert.assertEquals(p, backToObject);
	}

	@Test
	public void testSimpleBranchName() throws JsonProcessingException {
		CleanthatRefFilterProperties p = new CleanthatRefFilterProperties();

		p.setProtectedPatterns(Arrays.asList("branchName"));

		Assertions.assertThat(p.getProtectedPatterns()).containsExactly("refs/heads/branchName");
	}

	@Test
	public void testSimpleBranchPattern() throws JsonProcessingException {
		CleanthatRefFilterProperties p = new CleanthatRefFilterProperties();

		p.setProtectedPatterns(Arrays.asList("branchName"));

		Assertions.assertThat(p.getProtectedPatterns()).containsExactly("refs/heads/branchName");
	}

	@Test
	public void testSimpleBranchName_andQualifiedExplicitly() throws JsonProcessingException {
		CleanthatRefFilterProperties p = new CleanthatRefFilterProperties();

		p.setProtectedPatterns(Arrays.asList("branchName", "refs/heads/branchName"));

		Assertions.assertThat(p.getProtectedPatterns()).containsExactly("refs/heads/branchName");
	}

	@Test
	public void testSimpleBranchName_tag() throws JsonProcessingException {
		CleanthatRefFilterProperties p = new CleanthatRefFilterProperties();

		p.setProtectedPatterns(Arrays.asList("refs/tags/v1.0"));

		Assertions.assertThat(p.getProtectedPatterns()).containsExactly("refs/tags/v1.0");
	}

	@Test
	public void testSimpleBranchName_withDomain() throws JsonProcessingException {
		CleanthatRefFilterProperties p = new CleanthatRefFilterProperties();

		p.setProtectedPatterns(Arrays.asList("domain1/branchName1", "domain2/sub/branchName2"));

		Assertions.assertThat(p.getProtectedPatterns())
				.containsExactly("refs/heads/domain1/branchName1", "refs/heads/domain2/sub/branchName2");
	}
}
