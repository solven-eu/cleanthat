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

import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.formatter.LineEnding;
import org.junit.Assert;
import org.junit.Test;

public class TestSourceCodeProperties {
	@Test
	public void testDefaultConstructor() {
		SourceCodeProperties properties = new SourceCodeProperties();

		// By default, neither LR or CRLF as we should not privilege a platform
		// We rely on UNKNOWN so that any other parameter takes precedence
		Assert.assertEquals(LineEnding.UNKNOWN, properties.getLineEndingAsEnum());
	}
}
