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
package eu.solven.cleanthat.spotless.language;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class TestJavaFormatterStepFactory {
	@Test
	public void testParseVersion_release() {
		Map<?, ?> properties = ImmutableMap.of("project.version", "1.2.3");
		Assertions.assertThat(JavaFormatterStepFactory.cleanCleanthatVersionFromMvnProperties(properties))
				.isEqualTo("1.2.3");
	}

	@Test
	public void testParseVersion_snapshotAccepted() {
		System.setProperty(JavaFormatterStepFactory.ENV_CLEANTHAT_INCLUDE_DRAFT, "true");

		try {
			Map<?, ?> properties = ImmutableMap.of("project.version", "1.2-SNAPSHOT");
			Assertions.assertThat(JavaFormatterStepFactory.cleanCleanthatVersionFromMvnProperties(properties))
					.isEqualTo("1.2-SNAPSHOT");
		} finally {
			System.clearProperty(JavaFormatterStepFactory.ENV_CLEANTHAT_INCLUDE_DRAFT);
		}
	}

	// This typically happens on GithubApp: local version is a snapshot, but we want to execute Spotless on the previous
	// release
	// TODO Study how we could ensure Spotless relies on current version
	@Test
	public void testParseVersion_snapshotRejected() {
		Map<?, ?> properties = ImmutableMap.of("project.version", "1.10-SNAPSHOT");
		Assertions.assertThat(JavaFormatterStepFactory.cleanCleanthatVersionFromMvnProperties(properties))
				.isEqualTo("1.9");
	}
}
