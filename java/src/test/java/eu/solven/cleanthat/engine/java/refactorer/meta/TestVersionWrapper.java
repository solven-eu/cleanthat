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
package eu.solven.cleanthat.engine.java.refactorer.meta;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestVersionWrapper {
	@Test
	public void testCompareVersions() {
		Assertions.assertThat(new VersionWrapper(IJdkVersionConstants.JDK_7))
				.isGreaterThan(new VersionWrapper(IJdkVersionConstants.JDK_1))
				.isGreaterThan(new VersionWrapper(IJdkVersionConstants.JDK_6))
				.isLessThan(new VersionWrapper(IJdkVersionConstants.JDK_11));
	}
}
