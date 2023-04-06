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
package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.Arrays;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

public class TestCompositeMutator {
	@Test
	public void testTags() {
		IMutator a = Mockito.mock(IMutator.class);
		IMutator b = Mockito.mock(IMutator.class);

		Mockito.when(a.getTags()).thenReturn(Set.of("tagA", "tagB"));
		Mockito.when(b.getTags()).thenReturn(Set.of("tagB", "tagC"));

		CompositeMutator<IMutator> composite = new CompositeMutator<>(Arrays.asList(a, b));

		Assertions.assertThat(composite.getTags()).contains("tagB", "Composite").hasSize(2);
	}

	@Test
	public void testMinimalJdk() {
		IMutator a = Mockito.mock(IMutator.class);
		IMutator b = Mockito.mock(IMutator.class);

		// We pick 2 random versions
		Mockito.when(a.minimalJavaVersion()).thenReturn(IJdkVersionConstants.JDK_4);
		Mockito.when(b.minimalJavaVersion()).thenReturn(IJdkVersionConstants.JDK_15);

		CompositeMutator<IMutator> composite = new CompositeMutator<>(Arrays.asList(a, b));

		// This mutator is a no-op if the JDK is before the minimal underlying minimal version
		Assertions.assertThat(composite.minimalJavaVersion()).isEqualTo(IJdkVersionConstants.JDK_4);
	}
}
