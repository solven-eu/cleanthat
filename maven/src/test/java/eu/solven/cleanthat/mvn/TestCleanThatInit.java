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
package eu.solven.cleanthat.mvn;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestCleanThatInit {
	@Test
	public void testConfigUrlIsNull() {
		CleanThatInitMojo mojo = new CleanThatInitMojo();
		Assertions.assertThat(mojo.getConfigUrl()).isNull();
	}

	@Test
	public void testConfigUrlIsAnything() {
		CleanThatInitMojo mojo = new CleanThatInitMojo();
		mojo.setConfigUrl("anything");
		Assertions.assertThat(mojo.getConfigUrl()).isEqualTo("anything");
	}

	@Test
	public void testConfigUrlHasPlaceholder() {
		CleanThatInitMojo mojo = new CleanThatInitMojo();
		mojo.setConfigUrl("${not_replaced}");
		Assertions.assertThatThrownBy(() -> mojo.getConfigUrl()).isInstanceOf(IllegalStateException.class);
	}
}
