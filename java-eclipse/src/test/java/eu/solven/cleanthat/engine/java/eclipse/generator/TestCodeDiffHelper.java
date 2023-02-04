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
package eu.solven.cleanthat.engine.java.eclipse.generator;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestCodeDiffHelper {

	@Test
	public void testDeltaDiff() {
		CodeDiffHelper helper = new CodeDiffHelper();
		Assertions.assertThat(helper.deltaDiff("abcd", "efgh")).isEqualTo(4);

		Assertions.assertThat(helper.deltaDiff("abcd", "_abcd_")).isEqualTo(2);
		Assertions.assertThat(helper.deltaDiff("        b.add(-234);", "        b.add( -234);")).isEqualTo(1);

		Assertions.assertThat(helper.deltaDiff("aaa12bbb23ccc", "aaa  bbb  ccc")).isEqualTo(4);
	}
}
