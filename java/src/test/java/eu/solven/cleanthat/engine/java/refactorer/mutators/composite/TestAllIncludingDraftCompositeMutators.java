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

import org.assertj.core.api.Assertions;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.mutators.scanner.MutatorsScanner;

public class TestAllIncludingDraftCompositeMutators {

	@Before
	@After
	public void checkErrorCount() {
		Assertions.assertThat(MutatorsScanner.getErrorCount()).isZero();
	}

	@Test
	public void testScanComposite() {
		Assertions.assertThat(AllIncludingDraftCompositeMutators.ALL_INCLUDINGDRAFT.get())
				.map(m -> m.getName())
				.contains(PMDMutators.class.getName())
				.contains(AllIncludingDraftSingleMutators.class.getName())
				.contains(SafeButNotConsensualMutators.class.getName())
				.doesNotContain(AllIncludingDraftCompositeMutators.class.getName());
	}

	@Test
	public void testInstantiateManually() {
		var mutator = new AllIncludingDraftCompositeMutators(JavaVersion.parse(IJdkVersionConstants.LAST));

		Assertions.assertThat(mutator.getUnderlyings())
				.map(m -> m.getClass().getName())
				.contains(PMDMutators.class.getName())
				.contains(AllIncludingDraftSingleMutators.class.getName())
				.contains(SafeAndConsensualMutators.class.getName())
				.contains(SafeButNotConsensualMutators.class.getName())
				.doesNotContain(AllIncludingDraftCompositeMutators.class.getName())
				.contains(SafeButControversialMutators.class.getName())
				.contains(GuavaMutators.class.getName())
				.contains(ErrorProneMutators.class.getName())
				.contains(JSparrowMutators.class.getName());

		Assertions.assertThat(mutator.getUnderlyingIds()).contains("Guava");
	}
}
